package kr.hhplus.be.server.infrastructure.payment.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Optional<Payment> findById(Long id) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }
    @Override
    public Payment save(Payment payment) {
        // TODO Auto-generated method stub
        return null;
    }
}
