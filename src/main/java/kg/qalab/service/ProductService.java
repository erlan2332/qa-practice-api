package kg.qalab.service;

import kg.qalab.dto.product.PageResponse;
import kg.qalab.dto.product.ProductRequest;
import kg.qalab.dto.product.ProductResponse;
import kg.qalab.dto.product.StockRequest;
import kg.qalab.exception.ApiException;
import kg.qalab.mapper.ProductMapper;
import kg.qalab.model.Product;
import kg.qalab.repository.OrderRepository;
import kg.qalab.repository.ProductRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductService {

    private static final int MAX_PRODUCTS = 100;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductMapper productMapper;
    private final InMemoryTransactionManager transactionManager;

    public ProductService(
        ProductRepository productRepository,
        OrderRepository orderRepository,
        ProductMapper productMapper,
        InMemoryTransactionManager transactionManager
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.productMapper = productMapper;
        this.transactionManager = transactionManager;
    }

    public PageResponse<ProductResponse> findAll(
        String userId,
        String query,
        int page,
        int size
    ) {
        validateQuery(query, page, size);

        String normalizedQuery =
            query.toLowerCase(Locale.ROOT);

        List<Product> filtered = productRepository
            .findAllByUserId(userId)
            .stream()
            .filter(product ->
                product.name()
                    .toLowerCase(Locale.ROOT)
                    .contains(normalizedQuery)
            )
            .toList();

        List<ProductResponse> items = filtered
            .stream()
            .skip((long) page * size)
            .limit(size)
            .map(productMapper::toResponse)
            .toList();

        return new PageResponse<>(
            items,
            page,
            size,
            filtered.size()
        );
    }

    public ProductResponse findById(
        String userId,
        String productId
    ) {
        return productMapper.toResponse(
            requireProduct(userId, productId)
        );
    }

    public ProductResponse create(
        String userId,
        ProductRequest request
    ) {
        return transactionManager.execute(() -> {
            if (
                productRepository.countByUserId(userId)
                    >= MAX_PRODUCTS
            ) {
                throw new ApiException(
                    409,
                    "PRODUCT_LIMIT",
                    "Лимит: 100 товаров в песочнице"
                );
            }

            Product product = new Product(
                UUID.randomUUID().toString(),
                request.name().trim(),
                request.price(),
                request.stock(),
                "KGS"
            );

            productRepository.save(userId, product);

            return productMapper.toResponse(product);
        });
    }

    public ProductResponse replace(
        String userId,
        String productId,
        ProductRequest request
    ) {
        return transactionManager.execute(() -> {
            requireProduct(userId, productId);

            Product product = new Product(
                productId,
                request.name().trim(),
                request.price(),
                request.stock(),
                "KGS"
            );

            productRepository.save(userId, product);

            return productMapper.toResponse(product);
        });
    }

    public ProductResponse updateStock(
        String userId,
        String productId,
        StockRequest request
    ) {
        return transactionManager.execute(() -> {
            Product current =
                requireProduct(userId, productId);

            Product updated = new Product(
                current.id(),
                current.name(),
                current.price(),
                request.stock(),
                current.currency()
            );

            productRepository.save(userId, updated);

            return productMapper.toResponse(updated);
        });
    }

    public void delete(
        String userId,
        String productId
    ) {
        transactionManager.execute(() -> {
            requireProduct(userId, productId);

            if (
                orderRepository.existsCreatedByProductId(
                    userId,
                    productId
                )
            ) {
                throw new ApiException(
                    409,
                    "PRODUCT_RESERVED",
                    "Сначала отмените или оплатите активный заказ на этот товар"
                );
            }

            productRepository.delete(userId, productId);
        });
    }

    private Product requireProduct(
        String userId,
        String productId
    ) {
        return productRepository
            .findById(userId, productId)
            .orElseThrow(() ->
                new ApiException(
                    404,
                    "PRODUCT_NOT_FOUND",
                    "Товар не найден в вашей песочнице"
                )
            );
    }

    private void validateQuery(
        String query,
        int page,
        int size
    ) {
        if (
            page < 0
                || page > 10_000
                || size < 1
                || size > 50
                || query.length() > 80
        ) {
            throw new ApiException(
                400,
                "INVALID_QUERY",
                "page: 0–10000, size: 1–50, q: до 80 символов"
            );
        }
    }
}
