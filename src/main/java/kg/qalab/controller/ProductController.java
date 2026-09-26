package kg.qalab.controller;

import jakarta.validation.Valid;
import kg.qalab.dto.product.PageResponse;
import kg.qalab.dto.product.ProductRequest;
import kg.qalab.dto.product.ProductResponse;
import kg.qalab.dto.product.StockRequest;
import kg.qalab.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
        ProductService productService
    ) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> findAll(
        Authentication authentication,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return productService.findAll(
            authentication.getName(),
            q,
            page,
            size
        );
    }

    @GetMapping("/{id}")
    public ProductResponse findById(
        Authentication authentication,
        @PathVariable String id
    ) {
        return productService.findById(
            authentication.getName(),
            id
        );
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
        Authentication authentication,
        @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse product = productService.create(
            authentication.getName(),
            request
        );

        return ResponseEntity
            .created(
                URI.create(
                    "/api/products/" + product.id()
                )
            )
            .body(product);
    }

    @PutMapping("/{id}")
    public ProductResponse replace(
        Authentication authentication,
        @PathVariable String id,
        @Valid @RequestBody ProductRequest request
    ) {
        return productService.replace(
            authentication.getName(),
            id,
            request
        );
    }

    @PatchMapping("/{id}")
    public ProductResponse updateStock(
        Authentication authentication,
        @PathVariable String id,
        @Valid @RequestBody StockRequest request
    ) {
        return productService.updateStock(
            authentication.getName(),
            id,
            request
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        Authentication authentication,
        @PathVariable String id
    ) {
        productService.delete(
            authentication.getName(),
            id
        );

        return ResponseEntity.noContent().build();
    }
}
