package kr.hhplus.be.server.domain.order.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

// ============================================
// 주문 관리
// ============================================
public record Order(
    Long id,             // 식별자
    Long userId,         // 유저식별자
    Long couponId,       // 쿠폰식별자
    Long totalPrice,     // 총금액
    Long discountPrice,  // 할인금액
    Long finalPrice,     // 최종금액
    OrderStatus orderStatus,     // 주문상태
    LocalDateTime crtDttm,  // 생성일
    LocalDateTime updDttm   // 수정일
) {

    /**
     * 새로운 주문 생성
     */
    public static Order create(
            Long userId,
            Long couponId,
            Long totalPrice,
            Long discountPrice,
            Long finalPrice) {
        
        if (finalPrice < 0) {
            throw new BusinessException(ErrorCode.ORDER_FAILED, "생성");
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            null,
            userId,
            couponId,
            totalPrice,
            discountPrice,
            finalPrice,
            OrderStatus.PENDING,
            now,
            null
        );
    }

    /**
     * 주문 완료
     */
    public Order complete() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_FAILED, "완료");
        }
        
        return new Order(
            this.id,
            this.userId,
            this.couponId,
            this.totalPrice,
            this.discountPrice,
            this.finalPrice,
            OrderStatus.COMPLETED,
            this.crtDttm,
            LocalDateTime.now()
        );
    }

    /**
     * 주문 취소
     */
    public Order cancel() {
        if (!canCancel()) {
            throw new BusinessException(ErrorCode.ORDER_FAILED, 
                "취소할 수 없는 주문입니다. 현재 상태: " + this.orderStatus);
        }
        
        return new Order(
            this.id,
            this.userId,
            this.couponId,
            this.totalPrice,
            this.discountPrice,
            this.finalPrice,
            OrderStatus.CANCELED,
            this.crtDttm,
            LocalDateTime.now()
        );
    }

    public boolean canCancel() {
        return this.orderStatus == OrderStatus.PENDING 
            || this.orderStatus == OrderStatus.COMPLETED;
    }
}