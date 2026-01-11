package kr.hhplus.be.server.infrastructure.payment.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.from(payment);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpaRepository.findById(id).map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public List<Payment> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(PaymentEntity::toDomain)
                .collect(Collectors.toList());
    }
}
