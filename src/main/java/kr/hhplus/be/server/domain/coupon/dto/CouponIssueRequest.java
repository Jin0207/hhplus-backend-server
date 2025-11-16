package kr.hhplus.be.server.domain.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "쿠폰 발급 요청")
public record CouponIssueRequest(
    @Schema(description = "사용자 ID", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다")
    Long userId,
    
    @Schema(description = "쿠폰 ID", example = "1")
    @NotNull(message = "쿠폰 ID는 필수입니다")
    Long couponId
) {}
