package kg.qalab.dto.order;

import java.time.Instant;

public record OrderResponse(
    String id,
    String productId,
    String productName,
    int quantity,
    int unitPrice,
    long total,
    String currency,
    String status,
    Instant createdAt
) {
}
