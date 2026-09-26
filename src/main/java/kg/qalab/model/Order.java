package kg.qalab.model;

import java.time.Instant;

public record Order(
    String id,
    String productId,
    String productName,
    int quantity,
    int unitPrice,
    long total,
    String currency,
    OrderStatus status,
    Instant createdAt
) {
}
