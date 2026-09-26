package kg.qalab;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class LabStoreTest {
    @Test void concurrentOrdersNeverOversell() throws Exception {
        LabStore store=new LabStore(10,12);
        var session=store.register(new Api.Register("race@example.test","LearnOnly123!","QA"));
        String user=session.user().id();
        var product=store.createProduct(user,new Api.ProductInput("Last item",100,1));
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> task=()->{start.await();try {store.createOrder(user,new Api.OrderInput(product.id(),1),null);return 201;}catch(ApiException e){return e.status;}};
            Future<Integer> a=pool.submit(task),b=pool.submit(task);start.countDown();
            assertThat(List.of(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            assertThat(store.product(user,product.id()).stock()).isZero();
            assertThat(store.orders(user)).hasSize(1);
        }
    }
    @Test void expiredSessionRejected() {
        LabStore store=new LabStore(10,0);
        var session=store.register(new Api.Register("expired@example.test","LearnOnly123!","QA"));
        assertThat(store.authenticate(session.token())).isNull();
    }
    @Test void boundedUserAndProductCapacity() {
        LabStore store=new LabStore(1,12);
        var session=store.register(new Api.Register("one@example.test","LearnOnly123!","QA"));
        assertThatThrownBy(()->store.register(new Api.Register("two@example.test","LearnOnly123!","QA")))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.status).isEqualTo(503));
        for(int i=0;i<97;i++)store.createProduct(session.user().id(),new Api.ProductInput("P"+i,1,0));
        assertThatThrownBy(()->store.createProduct(session.user().id(),new Api.ProductInput("Overflow",1,0)))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("PRODUCT_LIMIT"));
    }
    @Test void atMostFiveTokensAndDeletedUserRevoked() {
        LabStore store=new LabStore(10,12);
        var session=store.register(new Api.Register("sessions@example.test","LearnOnly123!","QA"));
        for(int i=0;i<5;i++)store.login(new Api.Login("sessions@example.test","LearnOnly123!"));
        assertThat(store.authenticate(session.token())).isNull();
        var newest=store.login(new Api.Login("sessions@example.test","LearnOnly123!"));
        store.deleteAccount(session.user().id());
        assertThat(store.authenticate(newest.token())).isNull();
    }
}
