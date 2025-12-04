package kr.hhplus.be.server.application.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 생성
     */
    @Transactional
    public Payment createPayment(Long orderId, Long userId, Integer price, String paymentTypeString) {

        PaymentType paymentType = PaymentType.valueOf(paymentTypeString);

        Payment payment = Payment.create(orderId, userId, price, paymentType);
        return paymentRepository.save(payment);
    }

    /**
     * 결제 조회
     */
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, paymentId));
    }

    
    /**
     * 결제 완료
     */
    @Transactional
    public Payment completePayment(Long paymentId, String transactionId) {
        Payment payment = getPayment(paymentId);
        Payment completedPayment = payment.complete(payment, transactionId);
        return paymentRepository.save(completedPayment);
    }
}
