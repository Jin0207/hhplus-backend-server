package kr.hhplus.be.server.domain.product.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.product.enums.ProductCategory;

/**
 * 인기 상품 (배치 집계용)
 * 기준일(baseDate) 기준 직전 3일간(periodStartDate ~ periodEndDate) 판매량 집계 결과
 */
public record PopularProduct(
    Long id,                        // 식별자
    Integer rank,                   // 순위 (1~5)
    Long productId,                 // 상품 ID
    String productName,             // 상품명
    Long price,                     // 판매가격
    ProductCategory category,       // 카테고리
    Integer totalSalesQuantity,     // 집계 기간 총 판매량
    LocalDate baseDate,             // 기준일 (배치 실행일)
    LocalDate periodStartDate,      // 집계 시작일 (base_date - 3일)
    LocalDate periodEndDate,        // 집계 종료일 (base_date - 1일)
    LocalDateTime crtDttm           // 생성일시
) {

    /**
     * 신규 인기 상품 생성 (배치에서 사용)
     */
    public static PopularProduct create(
        Integer rank,
        Product product,
        Integer totalSalesQuantity,
        LocalDate baseDate
    ) {
        return new PopularProduct(
            null,
            rank,
            product.id(),
            product.productName(),
            product.price(),
            product.category(),
            totalSalesQuantity,
            baseDate,
            baseDate.minusDays(3),
            baseDate.minusDays(1),
            LocalDateTime.now()
        );
    }

    /**
     * 집계 결과로부터 인기 상품 생성 (배치에서 사용)
     */
    public static PopularProduct fromAggregation(
        Integer rank,
        Long productId,
        String productName,
        Long price,
        ProductCategory category,
        Integer totalSalesQuantity,
        LocalDate baseDate
    ) {
        return new PopularProduct(
            null,
            rank,
            productId,
            productName,
            price,
            category,
            totalSalesQuantity,
            baseDate,
            baseDate.minusDays(3),
            baseDate.minusDays(1),
            LocalDateTime.now()
        );
    }
}
