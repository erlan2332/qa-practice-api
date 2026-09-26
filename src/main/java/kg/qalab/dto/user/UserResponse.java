package kg.qalab.dto.user;

public record UserResponse(
    String id,
    String email,
    String name,
    String role
) {
}
