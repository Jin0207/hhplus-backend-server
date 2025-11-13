package kr.hhplus.be.server.domain.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포인트 조회 응답")
public record PointResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,
    
    @Schema(description = "현재 포인트", example = "15000")
    Integer currentPoint
) {}
