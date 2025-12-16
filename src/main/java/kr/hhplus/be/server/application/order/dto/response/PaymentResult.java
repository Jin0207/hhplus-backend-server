package kr.hhplus.be.server.application.order.dto.response;

import lombok.Builder;

@Builder
public record PaymentResult(
    boolean success,
    String transactionId,
    String failReason
) {
    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }
    
    public static PaymentResult fail(String reason) {
        return new PaymentResult(false, null, reason);
    }
    
    public boolean isSuccess() {
        return success;
    }
}
