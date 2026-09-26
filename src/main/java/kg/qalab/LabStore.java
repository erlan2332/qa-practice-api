package kg.qalab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static kg.qalab.Api.*;

/** Deliberately disposable storage, with one isolated workspace per learner.
 * A single monitor makes stock/order operations atomic for this small demo.
 * This is NOT a production database or a horizontally scalable service. */
@Service
public class LabStore {
    private static final int MAX_PRODUCTS = 100, MAX_ORDERS = 100;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(10);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Ticket> sessions = new HashMap<>();
    private final String dummyHash = passwords.encode("not-a-real-user-password");
    private final int maxUsers;
    private final int sessionHours;
    private final Instant startedAt = Instant.now();

    private static final class Account {
        User user;
        final String hash;
        Instant lastUsed = Instant.now();
        final Map<String,Product> products = new LinkedHashMap<>();
        final Map<String,Order> orders = new LinkedHashMap<>();
        final Map<String,Replay> replays = new HashMap<>();
        Account(User user, String hash) { this.user=user; this.hash=hash; }
    }
    private record Ticket(String userId, Instant expiresAt) {}
    private record Replay(OrderInput input, String orderId) {}
    public record CreatedOrder(Order order, boolean replayed) {}

    public LabStore(@Value("${app.max-users}") int maxUsers,
                    @Value("${app.session-hours}") int sessionHours) {
        this.maxUsers=maxUsers;
        this.sessionHours=sessionHours;
    }

    public synchronized Session register(Register input) {
        cleanup();
        String email = normalize(input.email());
        if (accounts.values().stream().anyMatch(a -> a.user.email().equals(email)))
            throw error(409,"EMAIL_EXISTS","Этот email уже зарегистрирован");
        if (accounts.size() >= maxUsers)
            throw error(503,"LAB_FULL","Песочница заполнена. Попробуйте позже или запустите её локально");
        validatePassword(input.password());
        Account account = new Account(new User(id(),email,input.name().trim(),"USER"), passwords.encode(input.password()));
        seed(account);
        accounts.put(account.user.id(),account);
        return issue(account);
    }

    public synchronized Session login(Login input) {
        Account account = accounts.values().stream().filter(a -> a.user.email().equals(normalize(input.email()))).findFirst().orElse(null);
        // BCrypt checks even for an unknown user, and never exposes which credential failed.
        boolean matches = input.password().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(input.password(),account == null ? dummyHash : account.hash);
        if (account == null || !matches) throw error(401,"INVALID_CREDENTIALS","Неверный email или пароль");
        account.lastUsed=Instant.now();
        return issue(account);
    }

    private Session issue(Account account) {
        Instant now=Instant.now();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        // Keep at most five active tokens per learner.
        var own=sessions.entrySet().stream().filter(e -> e.getValue().userId().equals(account.user.id()))
            .sorted(Comparator.comparing(e -> e.getValue().expiresAt())).toList();
        for (int i=0;i<=own.size()-5;i++) sessions.remove(own.get(i).getKey());
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires=now.plus(sessionHours,ChronoUnit.HOURS);
        sessions.put(digest(token),new Ticket(account.user.id(),expires));
        return new Session(token,"Bearer",expires,account.user);
    }

    public synchronized String authenticate(String token) {
        if (token.length()>128) return null;
        Ticket ticket=sessions.get(digest(token));
        if (ticket==null) return null;
        if (!ticket.expiresAt().isAfter(Instant.now()) || !accounts.containsKey(ticket.userId())) {
            sessions.remove(digest(token)); return null;
        }
        accounts.get(ticket.userId()).lastUsed=Instant.now();
        return ticket.userId();
    }
    public synchronized void logout(String token) { sessions.remove(digest(token)); }
    public synchronized User me(String userId) { return account(userId).user; }
    public synchronized User rename(String userId, Profile input) {
        Account a=account(userId);
        a.user=new User(a.user.id(),a.user.email(),input.name().trim(),a.user.role());
        return a.user;
    }
    public List<Product> catalog() {
        return List.of(new Product("demo-1","Блокнот тестировщика",250,10,"KGS"),
            new Product("demo-2","Кружка: у меня работает",650,5,"KGS"),
            new Product("demo-3","Стикеры BUG FOUND",120,0,"KGS"));
    }
    private void seed(Account a) {
        for (Product p:catalog()) {
            String id=id(); a.products.put(id,new Product(id,p.name(),p.price(),p.stock(),p.currency()));
        }
    }
    public synchronized Page<Product> products(String userId,String query,int page,int size) {
        if (page<0 || page>10000 || size<1 || size>50 || query.length()>80)
            throw error(400,"INVALID_QUERY","page: 0–10000, size: 1–50, q: до 80 символов");
        var all=account(userId).products.values().stream()
            .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        return new Page<>(all.stream().skip((long)page*size).limit(size).toList(),page,size,all.size());
    }
    public synchronized Product product(String userId,String id) {
        Product p=account(userId).products.get(id);
        if (p==null) throw error(404,"PRODUCT_NOT_FOUND","Товар не найден в вашей песочнице");
        return p;
    }
    public synchronized Product createProduct(String userId,ProductInput input) {
        Account a=account(userId);
        if (a.products.size()>=MAX_PRODUCTS) throw error(409,"PRODUCT_LIMIT","Лимит: 100 товаров в песочнице");
        Product p=new Product(id(),input.name().trim(),input.price(),input.stock(),"KGS");
        a.products.put(p.id(),p); return p;
    }
    public synchronized Product replaceProduct(String userId,String id,ProductInput input) {
        product(userId,id);
        Product p=new Product(id,input.name().trim(),input.price(),input.stock(),"KGS");
        account(userId).products.put(id,p); return p;
    }
    public synchronized Product stock(String userId,String id,StockInput input) {
        Product old=product(userId,id);
        Product p=new Product(id,old.name(),old.price(),input.stock(),old.currency());
        account(userId).products.put(id,p); return p;
    }
    public synchronized void deleteProduct(String userId,String id) {
        product(userId,id);
        if (account(userId).orders.values().stream().anyMatch(o -> o.productId().equals(id)&&o.status().equals("CREATED")))
            throw error(409,"PRODUCT_RESERVED","Сначала отмените или оплатите активный заказ на этот товар");
        account(userId).products.remove(id);
    }
    public synchronized CreatedOrder createOrder(String userId,OrderInput input,String key) {
        Account a=account(userId);
        if (key!=null && !key.matches("[A-Za-z0-9_-]{1,64}"))
            throw error(400,"INVALID_IDEMPOTENCY_KEY","Ключ: 1–64 латинских букв, цифр, _ или -");
        if (key!=null && a.replays.containsKey(key)) {
            Replay old=a.replays.get(key);
            if (!old.input().equals(input)) throw error(409,"IDEMPOTENCY_CONFLICT","Этот ключ уже использован для другого тела запроса");
            return new CreatedOrder(a.orders.get(old.orderId()),true);
        }
        if (a.orders.size()>=MAX_ORDERS) throw error(409,"ORDER_LIMIT","Лимит: 100 заказов. Сбросьте свою песочницу");
        Product p=product(userId,input.productId());
        if (p.stock()<input.quantity()) throw error(409,"OUT_OF_STOCK","Недостаточно товара на складе");
        Order o=new Order(id(),p.id(),p.name(),input.quantity(),p.price(),(long)p.price()*input.quantity(),"KGS","CREATED",Instant.now());
        a.products.put(p.id(),new Product(p.id(),p.name(),p.price(),p.stock()-input.quantity(),p.currency()));
        a.orders.put(o.id(),o);
        if (key!=null) a.replays.put(key,new Replay(input,o.id()));
        return new CreatedOrder(o,false);
    }
    public synchronized List<Order> orders(String userId) { return List.copyOf(account(userId).orders.values()); }
    public synchronized Order order(String userId,String id) {
        Order o=account(userId).orders.get(id);
        if (o==null) throw error(404,"ORDER_NOT_FOUND","Заказ не найден в вашей песочнице");
        return o;
    }
    public synchronized Order cancel(String userId,String id) {
        Order o=order(userId,id);
        if (o.status().equals("CANCELLED")) return o;
        if (!o.status().equals("CREATED")) throw error(409,"INVALID_ORDER_STATE","Оплаченный заказ нельзя отменить в этой учебной модели");
        Product p=product(userId,o.productId());
        if (p.stock()+o.quantity()>1000) throw error(409,"STOCK_LIMIT","Возврат превысит лимит склада 1000. Сначала уменьшите остаток");
        account(userId).products.put(p.id(),new Product(p.id(),p.name(),p.price(),p.stock()+o.quantity(),p.currency()));
        return saveStatus(userId,o,"CANCELLED");
    }
    public synchronized Order pay(String userId,String id,Payment input) {
        Order o=order(userId,id);
        if (!o.status().equals("CREATED")) throw error(409,"INVALID_ORDER_STATE","Оплатить можно только заказ CREATED");
        if (input.outcome().equals("DECLINED")) throw error(402,"PAYMENT_DECLINED","Учебный платёж отклонён. Реальных списаний нет");
        return saveStatus(userId,o,"PAID");
    }
    private Order saveStatus(String userId,Order o,String status) {
        Order updated=new Order(o.id(),o.productId(),o.productName(),o.quantity(),o.unitPrice(),o.total(),o.currency(),status,o.createdAt());
        account(userId).orders.put(o.id(),updated); return updated;
    }
    public synchronized void reset(String userId) {
        Account a=account(userId); a.products.clear(); a.orders.clear(); a.replays.clear(); seed(a);
    }
    public synchronized void deleteAccount(String userId) {
        account(userId); accounts.remove(userId);
        sessions.entrySet().removeIf(e -> e.getValue().userId().equals(userId));
    }
    public Instant startedAt() { return startedAt; }
    private Account account(String id) {
        Account a=accounts.get(id);
        if (a==null) throw error(401,"UNAUTHORIZED","Войдите заново");
        return a;
    }
    @Scheduled(fixedDelay=600000)
    public synchronized void cleanup() {
        Instant cutoff=Instant.now().minus(24,ChronoUnit.HOURS);
        accounts.entrySet().removeIf(e -> e.getValue().lastUsed.isBefore(cutoff));
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(Instant.now()) || !accounts.containsKey(e.getValue().userId()));
    }
    private static void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length>72)
            throw error(400,"PASSWORD_TOO_LONG","Пароль должен занимать не больше 72 байт UTF-8");
    }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static String id() { return UUID.randomUUID().toString(); }
    private static ApiException error(int status,String code,String message) { return new ApiException(status,code,message); }
}
