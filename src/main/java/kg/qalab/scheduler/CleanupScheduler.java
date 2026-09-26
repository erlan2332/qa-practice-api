package kg.qalab.scheduler;

import kg.qalab.service.CleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupScheduler {

    private final CleanupService cleanupService;

    public CleanupScheduler(
        CleanupService cleanupService
    ) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(fixedDelay = 600_000)
    public void cleanup() {
        cleanupService.cleanup();
    }
}
