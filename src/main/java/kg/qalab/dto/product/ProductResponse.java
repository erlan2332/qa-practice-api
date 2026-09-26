package kg.qalab.dto.product;

public record ProductResponse(
    String id,
    String name,
    int price,
    int stock,
    String currency
) {
}
