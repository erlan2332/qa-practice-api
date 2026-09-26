package kg.qalab.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(
    @NotNull
    @Min(0)
    @Max(1000)
    Integer stock
) {
}
