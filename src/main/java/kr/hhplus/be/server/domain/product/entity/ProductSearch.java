package kr.hhplus.be.server.domain.product.entity;

import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;

// ============================================
// 상품 목록 조회조건
// ============================================
public record ProductSearch(
        String productName,
        Integer minPrice,
        Integer maxPrice,
        ProductCategory category,
        ProductStatus status
) {
}