package kg.qalab.controller;

import jakarta.validation.Valid;
import kg.qalab.dto.auth.LoginRequest;
import kg.qalab.dto.auth.RegisterRequest;
import kg.qalab.dto.auth.SessionResponse;
import kg.qalab.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<SessionResponse> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity
            .created(URI.create("/api/me"))
            .body(authService.register(request));
    }

    @PostMapping("/login")
    public SessionResponse login(
        @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestHeader("Authorization")
        String authorization
    ) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
