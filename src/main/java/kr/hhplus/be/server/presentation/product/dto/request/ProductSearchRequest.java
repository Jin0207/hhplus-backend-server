package kr.hhplus.be.server.presentation.product.dto.request;

import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;

public record ProductSearchRequest(
        String productName,
        Integer minPrice,
        Integer maxPrice,
        ProductCategory category,
        ProductStatus status,
        Integer page,
        Integer size
) {
    public ProductSearchCommand toCommand() {
        return new ProductSearchCommand(productName, category, status, minPrice, maxPrice, page, size);
    }
}

