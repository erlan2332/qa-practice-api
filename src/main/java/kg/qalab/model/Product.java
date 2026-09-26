package kg.qalab.model;

public record Product(
    String id,
    String name,
    int price,
    int stock,
    String currency
) {
}
