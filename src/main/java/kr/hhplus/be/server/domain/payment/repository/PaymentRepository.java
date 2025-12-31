package kr.hhplus.be.server.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.domain.payment.entity.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);

    /**
     * idempotencyKey로 결제 조회 (중복 확인용)
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * 사용자의 모든 결제 내역 조회
     */
    List<Payment> findByUserId(Long userId);
}
