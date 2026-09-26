package kg.qalab.repository;

import kg.qalab.model.Order;
import kg.qalab.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class OrderRepository {

    private final Map<String, Map<String, Order>> ordersByUser =
        new ConcurrentHashMap<>();

    public List<Order> findAllByUserId(String userId) {
        Map<String, Order> orders = ordersByUser.get(userId);

        if (orders == null) {
            return List.of();
        }

        synchronized (orders) {
            return List.copyOf(orders.values());
        }
    }

    public Optional<Order> findById(
        String userId,
        String orderId
    ) {
        Map<String, Order> orders = ordersByUser.get(userId);

        if (orders == null) {
            return Optional.empty();
        }

        synchronized (orders) {
            return Optional.ofNullable(orders.get(orderId));
        }
    }

    public int countByUserId(String userId) {
        Map<String, Order> orders = ordersByUser.get(userId);

        if (orders == null) {
            return 0;
        }

        synchronized (orders) {
            return orders.size();
        }
    }

    public boolean existsCreatedByProductId(
        String userId,
        String productId
    ) {
        return findAllByUserId(userId)
            .stream()
            .anyMatch(order ->
                order.productId().equals(productId)
                    && order.status() == OrderStatus.CREATED
            );
    }

    public Order save(String userId, Order order) {
        Map<String, Order> orders = workspace(userId);

        synchronized (orders) {
            orders.put(order.id(), order);
        }

        return order;
    }

    public void deleteAll(String userId) {
        ordersByUser.remove(userId);
    }

    private Map<String, Order> workspace(String userId) {
        return ordersByUser.computeIfAbsent(
            userId,
            key -> Collections.synchronizedMap(new LinkedHashMap<>())
        );
    }
}
