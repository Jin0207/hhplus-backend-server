package kr.hhplus.be.server.presentation.product.dto.request;

import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;

public record ProductSearchRequest(
        String productName,
        Integer minPrice,
        Integer maxPrice,
        String category,
        String status,
        Integer page,
        Integer size
) {
    public ProductSearchCommand toCommand() {
        return new ProductSearchCommand(productName, category, status, minPrice, maxPrice, page, size);
    }
}

