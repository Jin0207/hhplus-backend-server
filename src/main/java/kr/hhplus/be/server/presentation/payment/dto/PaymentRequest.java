package kr.hhplus.be.server.presentation.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
    @NotNull(message = "주문 ID는 필수입니다")
    Long orderId,
    
    @NotBlank(message = "결제 수단은 필수입니다")
    String paymentType,
    
    String paymentGateway
) {}