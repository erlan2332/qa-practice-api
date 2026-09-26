package kg.qalab.mapper;

import kg.qalab.dto.product.ProductResponse;
import kg.qalab.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.id(),
            product.name(),
            product.price(),
            product.stock(),
            product.currency()
        );
    }
}
