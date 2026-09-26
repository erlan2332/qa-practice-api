package kg.qalab.controller;

import jakarta.validation.Valid;
import kg.qalab.dto.order.OrderRequest;
import kg.qalab.dto.order.OrderResponse;
import kg.qalab.dto.order.PaymentRequest;
import kg.qalab.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Map<String, Object> findAll(
        Authentication authentication
    ) {
        return Map.of(
            "items",
            orderService.findAll(
                authentication.getName()
            )
        );
    }

    @GetMapping("/{id}")
    public OrderResponse findById(
        Authentication authentication,
        @PathVariable String id
    ) {
        return orderService.findById(
            authentication.getName(),
            id
        );
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
        Authentication authentication,
        @Valid @RequestBody OrderRequest request,
        @RequestHeader(
            value = "Idempotency-Key",
            required = false
        )
        String idempotencyKey
    ) {
        OrderService.CreatedOrder result =
            orderService.create(
                authentication.getName(),
                request,
                idempotencyKey
            );

        return ResponseEntity
            .status(result.replayed() ? 200 : 201)
            .location(
                URI.create(
                    "/api/orders/"
                        + result.order().id()
                )
            )
            .header(
                "Idempotency-Replayed",
                String.valueOf(result.replayed())
            )
            .body(result.order());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
        Authentication authentication,
        @PathVariable String id
    ) {
        return orderService.cancel(
            authentication.getName(),
            id
        );
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(
        Authentication authentication,
        @PathVariable String id,
        @Valid @RequestBody PaymentRequest request
    ) {
        return orderService.pay(
            authentication.getName(),
            id,
            request
        );
    }
}
