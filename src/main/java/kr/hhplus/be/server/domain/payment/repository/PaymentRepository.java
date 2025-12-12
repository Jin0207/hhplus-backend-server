package kr.hhplus.be.server.domain.payment.repository;

import java.util.Optional;

import kr.hhplus.be.server.domain.payment.entity.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
