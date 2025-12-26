package kr.hhplus.be.server.presentation.coupon.dto.response;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;

public record CouponResponse(
    Long couponId,
    String couponName,
    CouponType couponType,
    Long discountValue,
    Long minOrderPrice,
    Integer quantity,
    Integer availableQuantity,
    CouponStatus status,
    LocalDateTime validFrom,
    LocalDateTime validTo
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
            coupon.id(),
            coupon.name(),
            coupon.type(),
            coupon.discountValue(),
            coupon.minOrderPrice(),
            coupon.quantity(),
            coupon.availableQuantity(),
            coupon.status(),
            coupon.validFrom(),
            coupon.validTo()
        );
    }
}