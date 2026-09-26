package kg.qalab.dto.product;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    int total
) {
}
