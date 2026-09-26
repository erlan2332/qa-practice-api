package kg.qalab.repository;

import kg.qalab.model.IdempotencyRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IdempotencyRepository {

    private final Map<String, Map<String, IdempotencyRecord>> recordsByUser =
        new ConcurrentHashMap<>();

    public Optional<IdempotencyRecord> find(
        String userId,
        String key
    ) {
        Map<String, IdempotencyRecord> records =
            recordsByUser.get(userId);

        if (records == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(records.get(key));
    }

    public void save(IdempotencyRecord record) {
        recordsByUser
            .computeIfAbsent(
                record.userId(),
                key -> new ConcurrentHashMap<>()
            )
            .put(record.key(), record);
    }

    public void deleteAll(String userId) {
        recordsByUser.remove(userId);
    }
}
