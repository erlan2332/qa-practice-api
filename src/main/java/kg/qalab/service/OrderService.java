package kg.qalab.service;

import kg.qalab.dto.order.OrderRequest;
import kg.qalab.dto.order.OrderResponse;
import kg.qalab.dto.order.PaymentRequest;
import kg.qalab.exception.ApiException;
import kg.qalab.mapper.OrderMapper;
import kg.qalab.model.IdempotencyRecord;
import kg.qalab.model.Order;
import kg.qalab.model.OrderStatus;
import kg.qalab.model.PaymentOutcome;
import kg.qalab.model.Product;
import kg.qalab.repository.IdempotencyRepository;
import kg.qalab.repository.OrderRepository;
import kg.qalab.repository.ProductRepository;
import kg.qalab.storage.InMemoryTransactionManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final int MAX_ORDERS = 100;
    private static final int MAX_STOCK = 1000;

    private static final Pattern IDEMPOTENCY_KEY =
        Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OrderMapper orderMapper;
    private final InMemoryTransactionManager transactionManager;

    public OrderService(
        ProductRepository productRepository,
        OrderRepository orderRepository,
        IdempotencyRepository idempotencyRepository,
        OrderMapper orderMapper,
        InMemoryTransactionManager transactionManager
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.orderMapper = orderMapper;
        this.transactionManager = transactionManager;
    }

    public List<OrderResponse> findAll(String userId) {
        return orderRepository
            .findAllByUserId(userId)
            .stream()
            .map(orderMapper::toResponse)
            .toList();
    }

    public OrderResponse findById(
        String userId,
        String orderId
    ) {
        return orderMapper.toResponse(
            requireOrder(userId, orderId)
        );
    }

    public CreatedOrder create(
        String userId,
        OrderRequest request,
        String idempotencyKey
    ) {
        validateIdempotencyKey(idempotencyKey);

        return transactionManager.execute(() -> {
            if (idempotencyKey != null) {
                var replay = idempotencyRepository
                    .find(userId, idempotencyKey);

                if (replay.isPresent()) {
                    IdempotencyRecord old = replay.get();

                    if (
                        !old.matches(
                            request.productId(),
                            request.quantity()
                        )
                    ) {
                        throw new ApiException(
                            409,
                            "IDEMPOTENCY_CONFLICT",
                            "Этот ключ уже использован для другого тела запроса"
                        );
                    }

                    Order existing = requireOrder(
                        userId,
                        old.orderId()
                    );

                    return new CreatedOrder(
                        orderMapper.toResponse(existing),
                        true
                    );
                }
            }

            if (
                orderRepository.countByUserId(userId)
                    >= MAX_ORDERS
            ) {
                throw new ApiException(
                    409,
                    "ORDER_LIMIT",
                    "Лимит: 100 заказов. Сбросьте свою песочницу"
                );
            }

            Product product = requireProduct(
                userId,
                request.productId()
            );

            if (product.stock() < request.quantity()) {
                throw new ApiException(
                    409,
                    "OUT_OF_STOCK",
                    "Недостаточно товара на складе"
                );
            }

            Order order = new Order(
                UUID.randomUUID().toString(),
                product.id(),
                product.name(),
                request.quantity(),
                product.price(),
                (long) product.price() * request.quantity(),
                "KGS",
                OrderStatus.CREATED,
                Instant.now()
            );

            Product updatedProduct = new Product(
                product.id(),
                product.name(),
                product.price(),
                product.stock() - request.quantity(),
                product.currency()
            );

            productRepository.save(
                userId,
                updatedProduct
            );

            orderRepository.save(userId, order);

            if (idempotencyKey != null) {
                idempotencyRepository.save(
                    new IdempotencyRecord(
                        userId,
                        idempotencyKey,
                        request.productId(),
                        request.quantity(),
                        order.id()
                    )
                );
            }

            return new CreatedOrder(
                orderMapper.toResponse(order),
                false
            );
        });
    }

    public OrderResponse cancel(
        String userId,
        String orderId
    ) {
        return transactionManager.execute(() -> {
            Order order = requireOrder(userId, orderId);

            if (order.status() == OrderStatus.CANCELLED) {
                return orderMapper.toResponse(order);
            }

            if (order.status() != OrderStatus.CREATED) {
                throw new ApiException(
                    409,
                    "INVALID_ORDER_STATE",
                    "Оплаченный заказ нельзя отменить в этой учебной модели"
                );
            }

            Product product = requireProduct(
                userId,
                order.productId()
            );

            if (
                product.stock() + order.quantity()
                    > MAX_STOCK
            ) {
                throw new ApiException(
                    409,
                    "STOCK_LIMIT",
                    "Возврат превысит лимит склада 1000. Сначала уменьшите остаток"
                );
            }

            Product restoredProduct = new Product(
                product.id(),
                product.name(),
                product.price(),
                product.stock() + order.quantity(),
                product.currency()
            );

            productRepository.save(
                userId,
                restoredProduct
            );

            Order updatedOrder = withStatus(
                order,
                OrderStatus.CANCELLED
            );

            orderRepository.save(
                userId,
                updatedOrder
            );

            return orderMapper.toResponse(updatedOrder);
        });
    }

    public OrderResponse pay(
        String userId,
        String orderId,
        PaymentRequest request
    ) {
        return transactionManager.execute(() -> {
            Order order = requireOrder(userId, orderId);

            if (order.status() != OrderStatus.CREATED) {
                throw new ApiException(
                    409,
                    "INVALID_ORDER_STATE",
                    "Оплатить можно только заказ CREATED"
                );
            }

            PaymentOutcome outcome =
                PaymentOutcome.valueOf(request.outcome());

            if (outcome == PaymentOutcome.DECLINED) {
                throw new ApiException(
                    402,
                    "PAYMENT_DECLINED",
                    "Учебный платёж отклонён. Реальных списаний нет"
                );
            }

            Order updatedOrder = withStatus(
                order,
                OrderStatus.PAID
            );

            orderRepository.save(
                userId,
                updatedOrder
            );

            return orderMapper.toResponse(updatedOrder);
        });
    }

    private void validateIdempotencyKey(String key) {
        if (
            key != null
                && !IDEMPOTENCY_KEY.matcher(key).matches()
        ) {
            throw new ApiException(
                400,
                "INVALID_IDEMPOTENCY_KEY",
                "Ключ: 1–64 латинских букв, цифр, _ или -"
            );
        }
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

    private Order requireOrder(
        String userId,
        String orderId
    ) {
        return orderRepository
            .findById(userId, orderId)
            .orElseThrow(() ->
                new ApiException(
                    404,
                    "ORDER_NOT_FOUND",
                    "Заказ не найден в вашей песочнице"
                )
            );
    }

    private Order withStatus(
        Order order,
        OrderStatus status
    ) {
        return new Order(
            order.id(),
            order.productId(),
            order.productName(),
            order.quantity(),
            order.unitPrice(),
            order.total(),
            order.currency(),
            status,
            order.createdAt()
        );
    }

    public record CreatedOrder(
        OrderResponse order,
        boolean replayed
    ) {
    }
}
