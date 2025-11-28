package kr.hhplus.be.server.presentation.payment.dto;

import java.time.LocalDateTime;

public record PaymentResponse(
    Long paymentId,
    Long orderId,
    Integer price,
    String status,
    String transactionId,
    String paymentType,
    LocalDateTime successDttm
) {}