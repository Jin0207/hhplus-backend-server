package kr.hhplus.be.server.domain.product.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.product.enums.StockType;

// ============================================
// 상품 재고 관리
// ============================================
public record Stock(
    Long id,                 // 식별자
    Long productId,          // 상품식별자
    Integer quantity,           // 수량
    StockType stockType,           // 타입
    String reason,              // 입출고 사유
    LocalDateTime crtDttm       // 생성일
) {
    /**
     * 출고
     */
    public static Stock createOut(Long productId, Integer quatity, String reason){
        return new Stock(null, productId, quatity, StockType.OUT, reason, LocalDateTime.now());
    }
    /**
     * 입고
     */
    public static Stock createIn(Long productId, Integer quatity, String reason){
        return new Stock(null, productId, quatity, StockType.IN, reason, LocalDateTime.now());
    }

}