package kr.hhplus.be.server.application.payment.facade.interfaces;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.domain.payment.entity.Payment;

public interface PaymentProcessor {
    void validateIdempotencyKey(String idempotencyKey);
    PaymentResult processPayment(Payment payment, OrderCreateRequest request);
}
