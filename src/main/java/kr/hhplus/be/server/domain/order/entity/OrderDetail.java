package kr.hhplus.be.server.domain.order.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

// ============================================
// 주문 상세내역
// ============================================
public record OrderDetail(
    Long  id,             // 식별자
    Long  orderId,        // 주문식별자
    Long  productId,      // 상품식별자
    Integer quantity,       // 수량
    Integer unitPrice,      // 단가
    Integer subtotal,       // 소계
    LocalDateTime crtDttm,  // 생성일
    LocalDateTime updDttm   // 수정일
) {
    /**
    * 주문 상세 생성
    */
    public static OrderDetail create(
            Long orderId,
            Long productId,
            Integer quantity,
            Integer unitPrice) {
        
        if (quantity <= 0) {
            // 주문 상품은 최소 1개 이상이어야 합니다.
            throw new BusinessException(ErrorCode.ORDER_ITEM_EMPTY);
        }
        
        Integer subtotal = quantity * unitPrice;
        LocalDateTime now = LocalDateTime.now();
        
        return new OrderDetail(
            null,
            orderId,
            productId,
            quantity,
            unitPrice,
            subtotal,
            now,
            null
        );
    }
    /**
    * 주문 order ID 주입
    */
    public OrderDetail assignOrderId(Long orderId) {
        return new OrderDetail(
            id,
            orderId,
            productId,
            quantity,
            unitPrice,
            subtotal,
            crtDttm,
            null
        );
    }

}