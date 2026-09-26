package kg.qalab.model;

import java.time.Instant;

public record SessionToken(
    String userId,
    Instant expiresAt
) {
}
