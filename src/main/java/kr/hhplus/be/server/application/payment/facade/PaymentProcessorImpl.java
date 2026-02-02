package kr.hhplus.be.server.application.payment.facade;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.application.payment.facade.interfaces.PaymentProcessor;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 처리기
 * - 외부 PG사 연동 없음
 * - 사용자 포인트로만 결제
 * - 추후 PG사 연동시 transactionId 사용
 *   현재 null값 전달
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessorImpl implements PaymentProcessor{
    private final PaymentService paymentService;
    
    /**
     * 멱등성 키 검증 (Redis 분산락 + DB 검증)
     * @deprecated 분산락은 AOP로 처리하므로 validateIdempotencyKeyInDb 사용 권장
     */
    @Override
    @Deprecated
    public void validateIdempotencyKey(String idempotencyKey) {
        paymentService.checkForIdempotencyKey(idempotencyKey);
    }

    /**
     * DB에서만 멱등성 키 검증
     * 분산락은 @WithDistributedLock AOP에서 처리
     */
    @Override
    public void validateIdempotencyKeyInDb(String idempotencyKey) {
        paymentService.checkIdempotencyKeyInDb(idempotencyKey);
    }
    
    /**
     * 포인트 결제 처리
     * - 포인트는 이미 차감 완료된 상태
     */
    @Override
    public PaymentResult processPayment(Payment payment, OrderCreateRequest request) {
        log.info("포인트 결제 처리: paymentId={}, amount={}", 
            payment.id(), payment.price());
        
        // 포인트 결제는 이미 차감 완료했으므로 항상 성공
        // transactionId는 null (추후 PG사 연동 시 사용 예정)
        
        log.info("포인트 결제 성공: paymentId={}", payment.id());
        
        return PaymentResult.success(null);  // transactionId = null
    }
}
