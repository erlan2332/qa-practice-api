package kg.qalab.controller;

import kg.qalab.service.SandboxService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    private final SandboxService sandboxService;

    public SandboxController(
        SandboxService sandboxService
    ) {
        this.sandboxService = sandboxService;
    }

    @PostMapping("/reset")
    public Map<String, String> reset(
        Authentication authentication
    ) {
        sandboxService.reset(
            authentication.getName()
        );

        return Map.of(
            "message",
            "Ваша песочница сброшена. Товары получили новые id"
        );
    }
}
