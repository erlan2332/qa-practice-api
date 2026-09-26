package kg.qalab.service;

import kg.qalab.model.UserAccount;
import kg.qalab.repository.UserRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CleanupService {

    private final UserRepository userRepository;
    private final SandboxService sandboxService;
    private final TokenService tokenService;
    private final InMemoryTransactionManager transactionManager;

    public CleanupService(
        UserRepository userRepository,
        SandboxService sandboxService,
        TokenService tokenService,
        InMemoryTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.sandboxService = sandboxService;
        this.tokenService = tokenService;
        this.transactionManager = transactionManager;
    }

    public void cleanup() {
        transactionManager.execute(() -> {
            Instant cutoff = Instant.now()
                .minus(24, ChronoUnit.HOURS);

            List<UserAccount> inactiveUsers =
                userRepository.findInactiveBefore(cutoff);

            for (UserAccount user : inactiveUsers) {
                sandboxService.deleteUserData(user.id());
                tokenService.revokeAll(user.id());
                userRepository.deleteById(user.id());
            }

            tokenService.cleanupExpiredAndOrphaned();
        });
    }
}
