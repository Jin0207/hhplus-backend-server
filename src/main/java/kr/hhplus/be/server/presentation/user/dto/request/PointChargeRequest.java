package kr.hhplus.be.server.presentation.user.dto.request;

import jakarta.validation.constraints.*;

public record PointChargeRequest(
    @NotNull(message = "충전 금액은 필수입니다")
    @Min(value = 1, message = "충전 금액은 1 이상이어야 합니다")
    Integer amount
) {}
