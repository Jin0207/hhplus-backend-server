package kr.hhplus.be.server.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 요청")
public record PaymentRequest(
    @Schema(description = "주문 ID", example = "1")
    @NotNull(message = "주문 ID는 필수입니다")
    Long orderId,
    
    @Schema(description = "결제 수단", example = "CARD")
    @NotBlank(message = "결제 수단은 필수입니다")
    String paymentType,
    
    @Schema(description = "결제 게이트웨이", example = "토스페이")
    String paymentGateway
) {}