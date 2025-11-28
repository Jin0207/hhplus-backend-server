package kr.hhplus.be.server.domain.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PointChargeResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,
    
    @Schema(description = "충전 전 포인트", example = "5000")
    Integer beforePoint,
    
    @Schema(description = "충전 후 포인트", example = "15000")
    Integer afterPoint,
    
    @Schema(description = "충전 금액", example = "10000")
    Integer chargedAmount,
    
    @Schema(description = "메시지", example = "포인트가 성공적으로 충전되었습니다")
    String message
) {}
