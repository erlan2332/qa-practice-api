package kg.qalab.model;

import java.time.Instant;

public record UserAccount(
    String id,
    String email,
    String name,
    Role role,
    String passwordHash,
    Instant lastUsedAt
) {

    public UserAccount withName(String newName) {
        return new UserAccount(
            id,
            email,
            newName,
            role,
            passwordHash,
            lastUsedAt
        );
    }

    public UserAccount touch(Instant now) {
        return new UserAccount(
            id,
            email,
            name,
            role,
            passwordHash,
            now
        );
    }
}
