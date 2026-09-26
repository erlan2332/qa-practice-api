package kg.qalab.repository;

import kg.qalab.model.Product;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProductRepository {

    private final Map<String, Map<String, Product>> productsByUser =
        new ConcurrentHashMap<>();

    public List<Product> findAllByUserId(String userId) {
        Map<String, Product> products = productsByUser.get(userId);

        if (products == null) {
            return List.of();
        }

        synchronized (products) {
            return List.copyOf(products.values());
        }
    }

    public Optional<Product> findById(
        String userId,
        String productId
    ) {
        Map<String, Product> products = productsByUser.get(userId);

        if (products == null) {
            return Optional.empty();
        }

        synchronized (products) {
            return Optional.ofNullable(products.get(productId));
        }
    }

    public int countByUserId(String userId) {
        Map<String, Product> products = productsByUser.get(userId);

        if (products == null) {
            return 0;
        }

        synchronized (products) {
            return products.size();
        }
    }

    public Product save(String userId, Product product) {
        Map<String, Product> products = workspace(userId);

        synchronized (products) {
            products.put(product.id(), product);
        }

        return product;
    }

    public void delete(String userId, String productId) {
        Map<String, Product> products = productsByUser.get(userId);

        if (products == null) {
            return;
        }

        synchronized (products) {
            products.remove(productId);
        }
    }

    public void deleteAll(String userId) {
        productsByUser.remove(userId);
    }

    private Map<String, Product> workspace(String userId) {
        return productsByUser.computeIfAbsent(
            userId,
            key -> Collections.synchronizedMap(new LinkedHashMap<>())
        );
    }
}
