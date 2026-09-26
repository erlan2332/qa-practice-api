package kg.qalab.service;

import kg.qalab.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {

    private final AppProperties properties;
    private final Instant startedAt = Instant.now();

    public HealthService(AppProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("version", properties.getVersion());
        response.put("storage", "MEMORY");
        response.put("startedAt", startedAt);
        response.put(
            "message",
            "QA Lab готов к экспериментам"
        );
        return response;
    }

    public Instant startedAt() {
        return startedAt;
    }
}
