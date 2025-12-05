package kr.hhplus.be.server.domain.coupon.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

// ============================================
// 사용자 쿠폰현황
// ============================================
public record UserCoupon(
    Long userId,             // 사용자식별자
    Long couponId,           // 쿠폰식별자
    UserCouponStatus status,    // 상태
    LocalDateTime usedDttm,     // 사용일시
    LocalDateTime expiredDttm,  // 만료일시
    LocalDateTime crtDttm,      // 생성일
    LocalDateTime updDttm       // 수정일
) {
    /**
     * 사용자 쿠폰 발급
     */
    public static UserCoupon issue(Long userId, Long couponId, LocalDateTime validTo) {
        LocalDateTime now = LocalDateTime.now();
        return new UserCoupon(
            userId,
            couponId,
            UserCouponStatus.AVAILABLE,
            null,
            validTo,
            now,
            null
        );
    }

    /**
     * 쿠폰 사용
     */
    public UserCoupon use() {
        if (this.status != UserCouponStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(this.expiredDttm)) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        return new UserCoupon(
            this.userId,
            this.couponId,
            UserCouponStatus.USED,
            now,
            this.expiredDttm,
            this.crtDttm,
            now
        );
    }
    
    /**
     * 쿠폰 만료 처리
     */
    public UserCoupon expire() {
        return new UserCoupon(
            this.userId,
            this.couponId,
            UserCouponStatus.EXPIRED,
            this.usedDttm,
            this.expiredDttm,
            this.crtDttm,
            LocalDateTime.now()
        );
    }
}