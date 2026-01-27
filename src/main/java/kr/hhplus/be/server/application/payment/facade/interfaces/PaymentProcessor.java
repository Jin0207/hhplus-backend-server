package kr.hhplus.be.server.application.payment.facade.interfaces;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.domain.payment.entity.Payment;

public interface PaymentProcessor {
    /**
     * Redis 분산락 + DB 검증 (기존 방식)
     * @deprecated 분산락은 AOP로 처리하므로 validateIdempotencyKeyInDb 사용 권장
     */
    @Deprecated
    void validateIdempotencyKey(String idempotencyKey);

    /**
     * DB에서만 멱등성 키 검증 (분산락은 AOP에서 처리)
     */
    void validateIdempotencyKeyInDb(String idempotencyKey);

    PaymentResult processPayment(Payment payment, OrderCreateRequest request);
}
