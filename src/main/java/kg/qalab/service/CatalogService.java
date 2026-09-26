package kg.qalab.service;

import kg.qalab.dto.catalog.CatalogResponse;
import kg.qalab.mapper.ProductMapper;
import kg.qalab.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final ProductMapper productMapper;

    public CatalogService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<Product> defaultProducts() {
        return List.of(
            new Product(
                "demo-1",
                "Блокнот тестировщика",
                250,
                10,
                "KGS"
            ),
            new Product(
                "demo-2",
                "Кружка: у меня работает",
                650,
                5,
                "KGS"
            ),
            new Product(
                "demo-3",
                "Стикеры BUG FOUND",
                120,
                0,
                "KGS"
            )
        );
    }

    public CatalogResponse getCatalog() {
        return new CatalogResponse(
            defaultProducts()
                .stream()
                .map(productMapper::toResponse)
                .toList(),
            true
        );
    }
}
