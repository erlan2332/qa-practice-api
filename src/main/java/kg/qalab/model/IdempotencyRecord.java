package kg.qalab.model;

public record IdempotencyRecord(
    String userId,
    String key,
    String productId,
    int quantity,
    String orderId
) {

    public boolean matches(String productId, int quantity) {
        return this.productId.equals(productId)
            && this.quantity == quantity;
    }
}
