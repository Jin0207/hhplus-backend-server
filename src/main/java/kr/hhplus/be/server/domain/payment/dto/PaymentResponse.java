package kr.hhplus.be.server.domain.payment.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 응답")
public record PaymentResponse(
    @Schema(description = "결제 ID", example = "1")
    Long paymentId,
    
    @Schema(description = "주문 ID", example = "1")
    Long orderId,
    
    @Schema(description = "결제 금액", example = "2700000")
    Integer price,
    
    @Schema(description = "결제 상태", example = "COMPLETED")
    String status,
    
    @Schema(description = "거래 ID", example = "TX_20250112_001")
    String transactionId,
    
    @Schema(description = "결제 수단", example = "CARD")
    String paymentType,
    
    @Schema(description = "성공일시")
    LocalDateTime successDttm
) {}