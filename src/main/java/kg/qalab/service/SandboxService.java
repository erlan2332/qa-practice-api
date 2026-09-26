package kg.qalab.service;

import kg.qalab.model.Product;
import kg.qalab.repository.IdempotencyRepository;
import kg.qalab.repository.OrderRepository;
import kg.qalab.repository.ProductRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SandboxService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final CatalogService catalogService;
    private final InMemoryTransactionManager transactionManager;

    public SandboxService(
        ProductRepository productRepository,
        OrderRepository orderRepository,
        IdempotencyRepository idempotencyRepository,
        CatalogService catalogService,
        InMemoryTransactionManager transactionManager
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.catalogService = catalogService;
        this.transactionManager = transactionManager;
    }

    public void initializeUser(String userId) {
        transactionManager.execute(() -> seed(userId));
    }

    public void reset(String userId) {
        transactionManager.execute(() -> {
            deleteUserDataInternal(userId);
            seed(userId);
        });
    }

    public void deleteUserData(String userId) {
        transactionManager.execute(() ->
            deleteUserDataInternal(userId)
        );
    }

    private void deleteUserDataInternal(String userId) {
        productRepository.deleteAll(userId);
        orderRepository.deleteAll(userId);
        idempotencyRepository.deleteAll(userId);
    }

    private void seed(String userId) {
        for (Product template : catalogService.defaultProducts()) {
            String id = UUID.randomUUID().toString();

            productRepository.save(
                userId,
                new Product(
                    id,
                    template.name(),
                    template.price(),
                    template.stock(),
                    template.currency()
                )
            );
        }
    }
}
