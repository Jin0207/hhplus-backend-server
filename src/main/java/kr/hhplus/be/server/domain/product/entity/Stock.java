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
) {}