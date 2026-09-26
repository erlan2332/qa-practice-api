package kg.qalab.service;

import kg.qalab.config.AppProperties;
import kg.qalab.model.SessionToken;
import kg.qalab.model.UserAccount;
import kg.qalab.repository.SessionRepository;
import kg.qalab.repository.UserRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class TokenService {

    private static final int MAX_ACTIVE_TOKENS = 5;
    private static final int MAX_TOKEN_LENGTH = 128;

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AppProperties properties;
    private final InMemoryTransactionManager transactionManager;
    private final SecureRandom random = new SecureRandom();

    public TokenService(
        SessionRepository sessionRepository,
        UserRepository userRepository,
        AppProperties properties,
        InMemoryTransactionManager transactionManager
    ) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.transactionManager = transactionManager;
    }

    public IssuedToken issue(String userId) {
        return transactionManager.execute(() -> {
            cleanupExpiredAndOrphaned();

            List<SessionRepository.SessionEntry> ownSessions =
                sessionRepository.findByUserId(userId);

            int sessionsToRemove =
                ownSessions.size() - MAX_ACTIVE_TOKENS + 1;

            for (int i = 0; i < sessionsToRemove; i++) {
                sessionRepository.deleteByHash(
                    ownSessions.get(i).tokenHash()
                );
            }

            byte[] bytes = new byte[32];
            random.nextBytes(bytes);

            String rawToken = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

            Instant expiresAt = Instant.now().plus(
                properties.getSessionHours(),
                ChronoUnit.HOURS
            );

            sessionRepository.save(
                digest(rawToken),
                new SessionToken(userId, expiresAt)
            );

            return new IssuedToken(rawToken, expiresAt);
        });
    }

    public Optional<UserAccount> authenticate(String rawToken) {
        if (rawToken == null || rawToken.length() > MAX_TOKEN_LENGTH) {
            return Optional.empty();
        }

        String tokenHash = digest(rawToken);

        Optional<SessionToken> maybeSession =
            sessionRepository.findByHash(tokenHash);

        if (maybeSession.isEmpty()) {
            return Optional.empty();
        }

        SessionToken session = maybeSession.get();
        Instant now = Instant.now();

        if (!session.expiresAt().isAfter(now)) {
            sessionRepository.deleteByHash(tokenHash);
            return Optional.empty();
        }

        Optional<UserAccount> maybeUser =
            userRepository.findById(session.userId());

        if (maybeUser.isEmpty()) {
            sessionRepository.deleteByHash(tokenHash);
            return Optional.empty();
        }

        UserAccount touched = maybeUser.get().touch(now);
        userRepository.save(touched);

        return Optional.of(touched);
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.length() > MAX_TOKEN_LENGTH) {
            return;
        }

        sessionRepository.deleteByHash(digest(rawToken));
    }

    public void revokeAll(String userId) {
        sessionRepository.deleteByUserId(userId);
    }

    public void cleanupExpiredAndOrphaned() {
        sessionRepository.deleteExpiredOrOrphaned(
            Instant.now(),
            userRepository::existsById
        );
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest
                .getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record IssuedToken(
        String token,
        Instant expiresAt
    ) {
    }
}
