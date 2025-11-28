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
    public ProductSearch {
        // 검색 시 상태값이 null이면 자동으로 ON_SALE로 고정 (도메인 규칙)
        if (status == null) {
            status = ProductStatus.ON_SALE;
        }
    }
}