package kg.qalab.repository;

import kg.qalab.model.UserAccount;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>();

    public Optional<UserAccount> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<UserAccount> findByEmail(String email) {
        String userId = emailIndex.get(email);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    public boolean existsById(String id) {
        return users.containsKey(id);
    }

    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(email);
    }

    public int count() {
        return users.size();
    }

    public UserAccount save(UserAccount user) {
        UserAccount previous = users.put(user.id(), user);

        if (previous != null && !previous.email().equals(user.email())) {
            emailIndex.remove(previous.email(), previous.id());
        }

        emailIndex.put(user.email(), user.id());
        return user;
    }

    public void deleteById(String userId) {
        UserAccount removed = users.remove(userId);
        if (removed != null) {
            emailIndex.remove(removed.email(), removed.id());
        }
    }

    public List<UserAccount> findInactiveBefore(Instant cutoff) {
        List<UserAccount> result = new ArrayList<>();

        for (UserAccount user : users.values()) {
            if (user.lastUsedAt().isBefore(cutoff)) {
                result.add(user);
            }
        }

        return result;
    }
}
