package kr.hhplus.be.server.presentation.payment.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import lombok.Builder;

@Schema(description = "결제 정보를 담고 있는 클래스")
@Builder
public record PaymentResponse(
    Long paymentId,
    Long orderId,
    Long price,
    String status,
    String transactionId,
    String paymentType,
    LocalDateTime successDttm
) {
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
            .paymentId(payment.id())
            .orderId(payment.orderId())
            .price(payment.price())
            .status(payment.status().name())
            .transactionId(payment.transactionId())
            .paymentType(payment.paymentType().name())
            .successDttm(payment.successDttm())
            .build();
    }

}