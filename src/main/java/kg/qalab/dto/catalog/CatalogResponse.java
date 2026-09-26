package kg.qalab.dto.catalog;

import kg.qalab.dto.product.ProductResponse;

import java.util.List;

public record CatalogResponse(
    List<ProductResponse> items,
    boolean readOnly
) {
}
