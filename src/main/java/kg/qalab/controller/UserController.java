package kg.qalab.controller;

import jakarta.validation.Valid;
import kg.qalab.dto.user.UpdateProfileRequest;
import kg.qalab.dto.user.UserResponse;
import kg.qalab.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(Authentication authentication) {
        return userService.get(authentication.getName());
    }

    @PatchMapping
    public UserResponse rename(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.rename(
            authentication.getName(),
            request
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
        Authentication authentication
    ) {
        userService.deleteAccount(
            authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
