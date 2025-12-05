package kr.hhplus.be.server.domain.coupon.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

// ============================================
// 쿠폰 관리
// ============================================
public record Coupon(
    Long id,                 // 식별자
    String name,                // 쿠폰명
    CouponType type,            // 할인타입
    Integer discountValue,      // 할인금액/%
    Integer minOrderPrice,      // 최소주문금액
    LocalDateTime validFrom,    // 유효시작일
    LocalDateTime validTo,      // 유효종료일
    Integer quantity,           // 총발행수량
    Integer availableQuantity,  // 남은수량
    CouponStatus status,        // 상태
    LocalDateTime crtDttm,      // 생성일
    LocalDateTime updDttm       // 수정일
) {
    /*
    *   쿠폰 발급 가능여부 확인
    */
    public boolean canIssue(){
        LocalDateTime now = LocalDateTime.now();
        return this.status == CouponStatus.ACTIVE
            && this.availableQuantity > 0
            && now.isAfter(this.validFrom)
            && now.isBefore(this.validTo);
    }

    /*
    *   쿠폰 수량차감
    */
    public Coupon decreaseQuantity(){
        return new Coupon(
            this.id,
            this.name,
            this.type,
            this.discountValue,
            this.minOrderPrice,
            this.validFrom,
            this.validTo,
            this.quantity,
            this.availableQuantity - 1,
            this.status,
            this.crtDttm,
            LocalDateTime.now()
        );
    }

    /*
    *   할인 금액 계산
    */
    public Integer calculateDiscountAmount(Integer orderPrice){
        if(orderPrice < this.minOrderPrice){
            throw new BusinessException(ErrorCode.COUPON_MIN_OREDER_PRICE_NOT_MET, this.minOrderPrice);
        }

        if(this.type == CouponType.AMOUNT){
            // 금액 할인: 할인 금액만큼 차감 (주문 금액보다 클 수 없음)
            return Math.min(this.discountValue, orderPrice);
        }else{
             // 퍼센트 할인: 주문 금액의 N% 차감   
             return (orderPrice * this.discountValue) / 100;
        }
    }
    
    /**
     * 최종 결제 금액 계산
     */
    public Integer calculateFinalPrice(Integer orderPrice) {
        Integer discountAmount = calculateDiscountAmount(orderPrice);
        return orderPrice - discountAmount;
    }
}