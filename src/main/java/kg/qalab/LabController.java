package kg.qalab;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import static kg.qalab.Api.*;

@RestController
@RequestMapping("/api")
public class LabController {
    private final LabStore store;
    private final String version;
    public LabController(LabStore store,@Value("${app.version}") String version) { this.store=store; this.version=version; }
    @GetMapping("/health") public Map<String,Object> health() {
        return Map.of("status","UP","version",version,"storage","MEMORY","startedAt",store.startedAt(),"message","QA Lab готов к экспериментам");
    }
    @GetMapping("/catalog") public Map<String,Object> catalog() { return Map.of("items",store.catalog(),"readOnly",true); }
    @PostMapping("/auth/register") public ResponseEntity<Session> register(@Valid @RequestBody Register input) {
        return ResponseEntity.created(URI.create("/api/me")).body(store.register(input));
    }
    @PostMapping("/auth/login") public Session login(@Valid @RequestBody Login input) { return store.login(input); }
    @PostMapping("/auth/logout") public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        store.logout(authorization.substring(7)); return ResponseEntity.noContent().build();
    }
    @GetMapping("/me") public User me(Authentication auth) { return store.me(auth.getName()); }
    @PatchMapping("/me") public User rename(Authentication auth,@Valid @RequestBody Profile input) { return store.rename(auth.getName(),input); }
    @DeleteMapping("/me") public ResponseEntity<Void> deleteAccount(Authentication auth) { store.deleteAccount(auth.getName()); return ResponseEntity.noContent().build(); }
    @GetMapping("/products") public Page<Product> products(Authentication auth,@RequestParam(defaultValue="") String q,
                @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return store.products(auth.getName(),q,page,size);
    }
    @GetMapping("/products/{id}") public Product product(Authentication auth,@PathVariable String id) { return store.product(auth.getName(),id); }
    @PostMapping("/products") public ResponseEntity<Product> createProduct(Authentication auth,@Valid @RequestBody ProductInput input) {
        Product p=store.createProduct(auth.getName(),input); return ResponseEntity.created(URI.create("/api/products/"+p.id())).body(p);
    }
    @PutMapping("/products/{id}") public Product replaceProduct(Authentication auth,@PathVariable String id,@Valid @RequestBody ProductInput input) { return store.replaceProduct(auth.getName(),id,input); }
    @PatchMapping("/products/{id}") public Product stock(Authentication auth,@PathVariable String id,@Valid @RequestBody StockInput input) { return store.stock(auth.getName(),id,input); }
    @DeleteMapping("/products/{id}") public ResponseEntity<Void> deleteProduct(Authentication auth,@PathVariable String id) { store.deleteProduct(auth.getName(),id); return ResponseEntity.noContent().build(); }
    @GetMapping("/orders") public Map<String,Object> orders(Authentication auth) { return Map.of("items",store.orders(auth.getName())); }
    @GetMapping("/orders/{id}") public Order order(Authentication auth,@PathVariable String id) { return store.order(auth.getName(),id); }
    @PostMapping("/orders") public ResponseEntity<Order> createOrder(Authentication auth,@Valid @RequestBody OrderInput input,
                @RequestHeader(value="Idempotency-Key",required=false) String key) {
        LabStore.CreatedOrder result=store.createOrder(auth.getName(),input,key);
        return ResponseEntity.status(result.replayed()?200:201).location(URI.create("/api/orders/"+result.order().id()))
            .header("Idempotency-Replayed",String.valueOf(result.replayed())).body(result.order());
    }
    @PostMapping("/orders/{id}/cancel") public Order cancel(Authentication auth,@PathVariable String id) { return store.cancel(auth.getName(),id); }
    @PostMapping("/orders/{id}/pay") public Order pay(Authentication auth,@PathVariable String id,@Valid @RequestBody Payment input) { return store.pay(auth.getName(),id,input); }
    @PostMapping("/sandbox/reset") public Map<String,String> reset(Authentication auth) { store.reset(auth.getName()); return Map.of("message","Ваша песочница сброшена. Товары получили новые id"); }
    @GetMapping("/admin/stats") public Map<String,String> admin() { return Map.of("message","В публичной версии роли ADMIN нет. Ожидаемый результат для USER — 403"); }
    @PostMapping("/lab/echo") public Map<String,Object> echo(@Valid @RequestBody Echo input,HttpServletRequest request) {
        return Map.of("method",request.getMethod(),"path",request.getRequestURI(),"body",input,"receivedAt",Instant.now());
    }
}
