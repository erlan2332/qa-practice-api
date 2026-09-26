package kg.qalab.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
    @NotNull
    @Pattern(regexp = "APPROVED|DECLINED")
    String outcome
) {
}
