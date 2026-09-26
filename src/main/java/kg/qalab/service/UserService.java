package kg.qalab.service;

import kg.qalab.dto.user.UpdateProfileRequest;
import kg.qalab.dto.user.UserResponse;
import kg.qalab.exception.ApiException;
import kg.qalab.mapper.UserMapper;
import kg.qalab.model.UserAccount;
import kg.qalab.repository.UserRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SandboxService sandboxService;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final InMemoryTransactionManager transactionManager;

    public UserService(
        UserRepository userRepository,
        SandboxService sandboxService,
        TokenService tokenService,
        UserMapper userMapper,
        InMemoryTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.sandboxService = sandboxService;
        this.tokenService = tokenService;
        this.userMapper = userMapper;
        this.transactionManager = transactionManager;
    }

    public UserResponse get(String userId) {
        return userMapper.toResponse(requireUser(userId));
    }

    public UserResponse rename(
        String userId,
        UpdateProfileRequest request
    ) {
        return transactionManager.execute(() -> {
            UserAccount current = requireUser(userId);

            UserAccount updated = current.withName(
                request.name().trim()
            );

            userRepository.save(updated);

            return userMapper.toResponse(updated);
        });
    }

    public void deleteAccount(String userId) {
        transactionManager.execute(() -> {
            requireUser(userId);
            sandboxService.deleteUserData(userId);
            tokenService.revokeAll(userId);
            userRepository.deleteById(userId);
        });
    }

    private UserAccount requireUser(String userId) {
        return userRepository
            .findById(userId)
            .orElseThrow(() ->
                new ApiException(
                    401,
                    "UNAUTHORIZED",
                    "Войдите заново"
                )
            );
    }
}
