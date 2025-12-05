package kr.hhplus.be.server.presentation.coupon.dto.response;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;

public record UserCouponResponse(
    Long userId,
    Long couponId,
    String couponName,
    CouponType couponType,
    Integer discountValue,
    Integer minOrderPrice,
    UserCouponStatus status,
    LocalDateTime usedDttm,
    LocalDateTime expiredDttm,
    LocalDateTime issuedDttm,
    boolean isExpired,
    boolean isUsable
) {
    public static UserCouponResponse from(UserCoupon userCoupon, Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = now.isAfter(userCoupon.expiredDttm());
        boolean isUsable = userCoupon.status() == UserCouponStatus.AVAILABLE 
            && !isExpired;

        return new UserCouponResponse(
            userCoupon.userId(),
            userCoupon.couponId(),
            coupon.name(),
            coupon.type(),
            coupon.discountValue(),
            coupon.minOrderPrice(),
            userCoupon.status(),
            userCoupon.usedDttm(),
            userCoupon.expiredDttm(),
            userCoupon.crtDttm(),
            isExpired,
            isUsable
        );
    }
}
