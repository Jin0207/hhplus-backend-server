package kr.hhplus.be.server.application.order.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 가격 계산기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPriceCalculator {
    private final CouponService couponService;

    public OrderPrice calculate(
            Long userId,
            List<OrderDetail> orderDetails, 
            OrderCreateRequest request) {
        
        // 1. 총 상품금액 계산
        Integer totalPrice = calculateTotalPrice(orderDetails);
        
        // 2. 쿠폰 할인 적용
        Long couponId = null;
        Integer discountPrice = 0;
        if (request.couponId() != null) {
            //쿠폰 조회 및 할인 금액 계산
            couponId = request.couponId();
            Coupon coupon = couponService.getCoupon(userId, couponId);

            discountPrice = coupon.calculateDiscountAmount(totalPrice);
            
            log.debug("쿠폰 할인 계산: couponId={}, totalPrice={}, discountAmount={}", 
                couponId, totalPrice, discountPrice);
        }

        // 최종 금액 = 총액 - 할인
        Integer finalPrice = totalPrice - discountPrice;
        
        // 유효성 검증
        if (finalPrice < 0) {
            throw new BusinessException(ErrorCode.FINAL_PRICE_THAN_ZERO);
        }
        
        log.debug("가격 계산 완료: total={}, discount={}, final={}", 
            totalPrice, discountPrice, finalPrice);
        
        return OrderPrice.builder()
            .totalPrice(totalPrice)
            .discountPrice(discountPrice)
            .couponId(couponId)
            .pointToUse(finalPrice)  // 최종 금액 = 사용할 포인트
            .finalPrice(finalPrice)
            .build();
    }
    
    private Integer calculateTotalPrice(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
            .mapToInt(detail -> detail.unitPrice() * detail.quantity())
            .sum();
    }
    
}
