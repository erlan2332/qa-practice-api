package kg.qalab.dto.auth;

import kg.qalab.dto.user.UserResponse;

import java.time.Instant;

public record SessionResponse(
    String token,
    String tokenType,
    Instant expiresAt,
    UserResponse user
) {
}
