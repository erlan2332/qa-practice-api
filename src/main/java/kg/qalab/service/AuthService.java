package kg.qalab.service;

import kg.qalab.config.AppProperties;
import kg.qalab.dto.auth.LoginRequest;
import kg.qalab.dto.auth.RegisterRequest;
import kg.qalab.dto.auth.SessionResponse;
import kg.qalab.exception.ApiException;
import kg.qalab.mapper.UserMapper;
import kg.qalab.model.Role;
import kg.qalab.model.UserAccount;
import kg.qalab.repository.UserRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final SandboxService sandboxService;
    private final TokenService tokenService;
    private final CleanupService cleanupService;
    private final AppProperties properties;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final InMemoryTransactionManager transactionManager;
    private final String dummyHash;

    public AuthService(
        UserRepository userRepository,
        SandboxService sandboxService,
        TokenService tokenService,
        CleanupService cleanupService,
        AppProperties properties,
        UserMapper userMapper,
        BCryptPasswordEncoder passwordEncoder,
        InMemoryTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.sandboxService = sandboxService;
        this.tokenService = tokenService;
        this.cleanupService = cleanupService;
        this.properties = properties;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.transactionManager = transactionManager;
        this.dummyHash = passwordEncoder.encode(
            "not-a-real-user-password"
        );
    }

    public SessionResponse register(RegisterRequest request) {
        cleanupService.cleanup();

        return transactionManager.execute(() -> {
            String email = normalizeEmail(request.email());

            if (userRepository.existsByEmail(email)) {
                throw new ApiException(
                    409,
                    "EMAIL_EXISTS",
                    "Этот email уже зарегистрирован"
                );
            }

            if (userRepository.count() >= properties.getMaxUsers()) {
                throw new ApiException(
                    503,
                    "LAB_FULL",
                    "Песочница заполнена. Попробуйте позже или запустите её локально"
                );
            }

            validatePassword(request.password());

            UserAccount user = new UserAccount(
                UUID.randomUUID().toString(),
                email,
                request.name().trim(),
                Role.USER,
                passwordEncoder.encode(request.password()),
                Instant.now()
            );

            userRepository.save(user);
            sandboxService.initializeUser(user.id());

            return createSession(user);
        });
    }

    public SessionResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        UserAccount user = userRepository
            .findByEmail(email)
            .orElse(null);

        boolean passwordLengthValid =
            request.password()
                .getBytes(StandardCharsets.UTF_8)
                .length <= BCRYPT_MAX_BYTES;

        boolean passwordMatches =
            passwordLengthValid
                && passwordEncoder.matches(
                    request.password(),
                    user == null
                        ? dummyHash
                        : user.passwordHash()
                );

        if (user == null || !passwordMatches) {
            throw new ApiException(
                401,
                "INVALID_CREDENTIALS",
                "Неверный email или пароль"
            );
        }

        UserAccount touched = user.touch(Instant.now());
        userRepository.save(touched);

        return createSession(touched);
    }

    public void logout(String authorization) {
        if (
            authorization == null
                || !authorization.startsWith("Bearer ")
        ) {
            throw new ApiException(
                401,
                "UNAUTHORIZED",
                "Добавьте действующий Authorization: Bearer <token>"
            );
        }

        tokenService.revoke(authorization.substring(7));
    }

    private SessionResponse createSession(UserAccount user) {
        TokenService.IssuedToken token =
            tokenService.issue(user.id());

        return new SessionResponse(
            token.token(),
            "Bearer",
            token.expiresAt(),
            userMapper.toResponse(user)
        );
    }

    private void validatePassword(String password) {
        int bytes = password
            .getBytes(StandardCharsets.UTF_8)
            .length;

        if (bytes > BCRYPT_MAX_BYTES) {
            throw new ApiException(
                400,
                "PASSWORD_TOO_LONG",
                "Пароль должен занимать не больше 72 байт UTF-8"
            );
        }
    }

    private String normalizeEmail(String email) {
        return email
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}
