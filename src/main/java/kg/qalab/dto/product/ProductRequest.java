package kg.qalab.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
    @NotBlank
    @Size(max = 80)
    String name,

    @NotNull
    @Min(1)
    @Max(1_000_000)
    Integer price,

    @NotNull
    @Min(0)
    @Max(1000)
    Integer stock
) {
}
