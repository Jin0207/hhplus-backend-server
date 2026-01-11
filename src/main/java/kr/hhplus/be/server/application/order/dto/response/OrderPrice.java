package kr.hhplus.be.server.application.order.dto.response;

import lombok.Builder;

@Builder
public record OrderPrice(
    Long totalPrice,
    Long discountPrice,
    Long couponId,
    Long pointToUse,
    Long finalPrice
) {}
