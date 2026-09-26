package kg.qalab.dto.lab;

import java.time.Instant;

public record EchoResponse(
    String method,
    String path,
    EchoRequest body,
    Instant receivedAt
) {
}
