package kr.hhplus.be.server.domain.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "포인트 충전 요청")
public record PointChargeRequest(
    @Schema(description = "충전할 포인트 금액", example = "10000")
    @NotNull(message = "충전 금액은 필수입니다")
    @Min(value = 1, message = "충전 금액은 1 이상이어야 합니다")
    Integer amount
) {}
