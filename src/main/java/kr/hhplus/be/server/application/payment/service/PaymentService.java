package kr.hhplus.be.server.application.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    
    /**
     * 멱등성 검사
     * 결제상태가 COMPLETED/PENDING 상태인 경우 여기서 예외를 던져서 상위 로직 실행을 차단
     */
    public void checkForIdempotencyKey(String idempotencyKey) {
         // 멱동성 키로 결제레코드 존재하는지 확인
        var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            PaymentStatus currentStatus = payment.status();

            if(currentStatus == PaymentStatus.COMPLETED){
                // 이미 처리된 결제
                throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
            }else if(currentStatus == PaymentStatus.PENDING){
                // 기존 결제상태 '완료'가 아닌 경우 중복 요청으로 간주
                throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
            }
        }
    }

    /**
     * 결제 생성
     * OrderFacade의 트랜잭션에서 호출되므로 @Transactional 불필요
     */
    public Payment createPayment(Long orderId, Long userId, String idempotencyKey, Long price, String paymentTypeString) {
        PaymentType paymentType = PaymentType.valueOf(paymentTypeString);

        Payment payment = Payment.create(orderId, userId, idempotencyKey, price, paymentType);
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
     * OrderFacade의 트랜잭션에서 호출되므로 @Transactional 불필요
     */
    public Payment completePayment(Long paymentId, String transactionId) {
        Payment payment = getPayment(paymentId);
        Payment completedPayment = payment.complete(transactionId);
        return paymentRepository.save(completedPayment);
    }

    /**
     * 결제 실패
     */
    @Transactional
    public Payment failPayment(Long paymentId, String failReason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        
        Payment failedPayment = payment.fail(failReason);
        return paymentRepository.save(failedPayment);
    }
}
