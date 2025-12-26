package kr.hhplus.be.server.domain.payment.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

// ============================================
// 결제 관리
// ============================================
public record Payment(
    Long  id,                 // 식별자
    Long  orderId,            // 주문식별자
    Long  userId,             // 유저식별자
    String idempotencyKey,   // 멱등성보장키
    Long price,              // 가격
    PaymentStatus status,       // 상태
    PaymentType paymentType,    // 유형
    String paymentGateway,      // 결제 게이트웨이(PG사)
    String transactionId,       // 거래 ID(PG사 제공)
    String failReason,          // 실패사유
    LocalDateTime requestDttm,  // 결제요청일시
    LocalDateTime successDttm,  // 결제성공일시
    Boolean externalSync,       // 외부시스템동기화여부
    LocalDateTime syncedDttm,   // 동기화일시
    LocalDateTime crtDttm,      // 생성일
    LocalDateTime updDttm       // 수정일
) {
    /**
     * 결제 생성
     */
    public static Payment create(Long orderId, Long userId, String idempotencyKey, Long price, PaymentType paymentType) {
        LocalDateTime now = LocalDateTime.now();
        
        return new Payment(
            null,
            orderId,
            userId,
            idempotencyKey,
            price,
            PaymentStatus.PENDING,
            paymentType,
            null,
            null,
            null,
            now,
            null,
            false,
            null,
            now,
            null
        );
    }

    /**
     * 결제 완료
     */
    public Payment complete(String trancsactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        return new Payment(
            this.id,
            this.orderId,
            this.userId,
            this.idempotencyKey,
            this.price,
            PaymentStatus.COMPLETED,
            this.paymentType,
            this.paymentGateway,
            transactionId,
            null,
            this.requestDttm,
            now,
            this.externalSync,
            (this.externalSync? now : null),
            this.crtDttm,
            now
        );
    }

    /**
     * 결제 실패
     */
    public Payment fail(String failReason) {
        
        LocalDateTime now = LocalDateTime.now();
        
        return new Payment(
            this.id,
            this.orderId,
            this.userId,
            this.idempotencyKey,
            this.price,
            PaymentStatus.FAILED,
            this.paymentType,
            this.paymentGateway,
            this.transactionId,
            failReason,
            this.requestDttm,
            null,
            this.externalSync,
            null,
            this.crtDttm,
            now
        );
    }

}
