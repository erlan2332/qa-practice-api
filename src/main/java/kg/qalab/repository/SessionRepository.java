package kg.qalab.repository;

import kg.qalab.model.SessionToken;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Repository
public class SessionRepository {

    private final Map<String, SessionToken> sessions =
        new ConcurrentHashMap<>();

    public Optional<SessionToken> findByHash(String tokenHash) {
        return Optional.ofNullable(sessions.get(tokenHash));
    }

    public void save(String tokenHash, SessionToken session) {
        sessions.put(tokenHash, session);
    }

    public void deleteByHash(String tokenHash) {
        sessions.remove(tokenHash);
    }

    public void deleteByUserId(String userId) {
        sessions.entrySet().removeIf(entry ->
            entry.getValue().userId().equals(userId)
        );
    }

    public List<SessionEntry> findByUserId(String userId) {
        List<SessionEntry> result = new ArrayList<>();

        sessions.forEach((hash, session) -> {
            if (session.userId().equals(userId)) {
                result.add(new SessionEntry(hash, session));
            }
        });

        result.sort(
            Comparator.comparing(
                entry -> entry.session().expiresAt()
            )
        );

        return result;
    }

    public void deleteExpiredOrOrphaned(
        Instant now,
        Predicate<String> userExists
    ) {
        sessions.entrySet().removeIf(entry -> {
            SessionToken session = entry.getValue();

            return !session.expiresAt().isAfter(now)
                || !userExists.test(session.userId());
        });
    }

    public record SessionEntry(
        String tokenHash,
        SessionToken session
    ) {
    }
}
