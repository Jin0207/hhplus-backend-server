package kr.hhplus.be.server.application.order.dto.response;

import lombok.Builder;

@Builder
public record OrderPrice(
    Integer totalPrice,
    Integer discountPrice,
    Long couponId,
    Integer pointToUse,
    Integer finalPrice
) {}
