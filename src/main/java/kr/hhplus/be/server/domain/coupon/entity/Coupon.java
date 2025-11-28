package kr.hhplus.be.server.domain.coupon.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;

// ============================================
// 쿠폰 관리
// ============================================
public record Coupon(
    Integer id,                 // 식별자
    String name,                // 쿠폰명
    CouponType type,            // 할인타입
    Integer discountValue,      // 할인금액/%
    Integer minOrderPrice,      // 최소주문금액
    LocalDateTime validFrom,    // 유효시작일
    LocalDateTime validTo,      // 유효종료일
    Integer quantity,           // 총발행수량
    Integer availableQuantity,  // 남은수량
    CouponStatus status,        // 상태
    LocalDateTime crtDttm,      // 생성일
    LocalDateTime updDttm       // 수정일
) {}