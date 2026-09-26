package kg.qalab.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderRequest(
    @NotBlank
    @Size(max = 36)
    String productId,

    @NotNull
    @Min(1)
    @Max(20)
    Integer quantity
) {
}
