package kr.hhplus.be.server.presentation.coupon.dto;

import java.time.LocalDateTime;

public record CouponIssueResponse(
    Long couponId,
    String couponName,
    String type,
    Integer discountValue,
    Integer minOrderPrice,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    String message
) {}