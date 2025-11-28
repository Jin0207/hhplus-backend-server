package kr.hhplus.be.server.domain.user.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.user.enums.UserCouponStatus;

// ============================================
// 사용자 쿠폰현황
// ============================================
public record UserCoupon(
    Integer userId,             // 사용자식별자
    Integer couponId,           // 쿠폰식별자
    UserCouponStatus status,    // 상태
    LocalDateTime usedDttm,     // 사용일시
    LocalDateTime expiredDttm,  // 만료일시
    LocalDateTime crtDttm,      // 생성일
    LocalDateTime updDttm       // 수정일
) {}