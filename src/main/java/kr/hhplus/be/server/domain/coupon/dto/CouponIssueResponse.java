package kr.hhplus.be.server.domain.coupon.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 발급 응답")
public record CouponIssueResponse(
    @Schema(description = "쿠폰 ID", example = "1")
    Long couponId,
    
    @Schema(description = "쿠폰명", example = "신규 회원 10% 할인")
    String couponName,
    
    @Schema(description = "할인 타입", example = "PERCENT")
    String type,
    
    @Schema(description = "할인 값", example = "10")
    Integer discountValue,
    
    @Schema(description = "최소 주문 금액", example = "50000")
    Integer minOrderPrice,
    
    @Schema(description = "유효 시작일")
    LocalDateTime validFrom,
    
    @Schema(description = "유효 종료일")
    LocalDateTime validTo,
    
    @Schema(description = "응답 메시지", example = "쿠폰이 성공적으로 발급되었습니다.")
    String message
) {}