package kr.hhplus.be.server.application.payment.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 멱등성 검사 (Redis 분산 락 사용)
     * 결제상태가 COMPLETED/PENDING/FAILED 상태인 경우 여기서 예외를 던져서 상위 로직 실행을 차단
     *
     * @deprecated 분산락은 AOP로 처리하므로 checkIdempotencyKeyInDb 사용 권장
     */
    @Deprecated
    public void checkForIdempotencyKey(String idempotencyKey) {
        String lockKey = "payment:idempotency:" + idempotencyKey;

        // Redis SET NX (존재하지 않으면 설정, 30초 TTL)
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));

        if (Boolean.FALSE.equals(lockAcquired)) {
            // 락을 획득하지 못함 = 동시 요청 또는 이미 처리된 요청
            // DB에서 결제 상태 확인하여 적절한 에러 반환
            var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                PaymentStatus currentStatus = payment.status();

                if(currentStatus == PaymentStatus.COMPLETED){
                    // 이미 처리된 결제
                    throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
                }
            }
            // DB에 없거나 PENDING/FAILED 상태면 중복 요청
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        // DB에서도 확인 (Redis 락 이후 이중 체크)
        checkIdempotencyKeyInDb(idempotencyKey);
    }

    /**
     * DB에서만 멱등성 키 검증 (분산락은 AOP에서 처리)
     * 이미 처리된 결제인지 DB 상태로 확인
     */
    public void checkIdempotencyKeyInDb(String idempotencyKey) {
        var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            PaymentStatus currentStatus = payment.status();

            if (currentStatus == PaymentStatus.COMPLETED) {
                // 이미 처리된 결제
                throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
            } else if (currentStatus == PaymentStatus.PENDING) {
                // 기존 결제상태 '완료'가 아닌 경우 중복 요청으로 간주
                throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
            } else if (currentStatus == PaymentStatus.FAILED) {
                // 실패한 결제의 멱등성 키는 재사용 불가
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
