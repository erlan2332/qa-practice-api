package kg.qalab.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/stats")
    public Map<String, String> stats() {
        return Map.of(
            "message",
            "В публичной версии роли ADMIN нет. Ожидаемый результат для USER — 403"
        );
    }
}
