package kg.qalab;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Small, explicit HTTP contracts. Prices are whole Kyrgyz soms (KGS). */
public final class Api {
    private Api() {}
    public record Register(@NotBlank @Email @Size(max=100) String email,
                           @NotBlank @Size(min=8,max=64) String password,
                           @NotBlank @Size(min=2,max=50) String name) {}
    public record Login(@NotBlank @Email @Size(max=100) String email,
                        @NotBlank @Size(max=64) String password) {}
    public record Profile(@NotBlank @Size(min=2,max=50) String name) {}
    public record User(String id, String email, String name, String role) {}
    public record Session(String token, String tokenType, Instant expiresAt, User user) {}
    public record ProductInput(@NotBlank @Size(max=80) String name,
                               @NotNull @Min(1) @Max(1000000) Integer price,
                               @NotNull @Min(0) @Max(1000) Integer stock) {}
    public record StockInput(@NotNull @Min(0) @Max(1000) Integer stock) {}
    public record Product(String id, String name, int price, int stock, String currency) {}
    public record Page<T>(List<T> items, int page, int size, int total) {}
    public record OrderInput(@NotBlank @Size(max=36) String productId,
                             @NotNull @Min(1) @Max(20) Integer quantity) {}
    public record Order(String id, String productId, String productName, int quantity,
                        int unitPrice, long total, String currency, String status, Instant createdAt) {}
    public record Payment(@NotNull @Pattern(regexp="APPROVED|DECLINED") String outcome) {}
    public record Echo(@NotBlank @Size(max=200) String message) {}
    public record Error(int status, String code, String message, Map<String,String> fields,
                        String requestId, Instant timestamp) {}
}
