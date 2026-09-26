package kg.qalab.dto.lab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EchoRequest(
    @NotBlank
    @Size(max = 200)
    String message
) {
}
