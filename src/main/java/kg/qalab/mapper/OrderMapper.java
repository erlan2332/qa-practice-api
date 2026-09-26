package kg.qalab.mapper;

import kg.qalab.dto.order.OrderResponse;
import kg.qalab.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.id(),
            order.productId(),
            order.productName(),
            order.quantity(),
            order.unitPrice(),
            order.total(),
            order.currency(),
            order.status().name(),
            order.createdAt()
        );
    }
}
