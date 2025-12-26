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
        Long totalPrice = calculateTotalPrice(orderDetails);
        
        // 2. 쿠폰 할인 계산
        Long couponId = request.couponId();
        Long discountPrice = 0L;

        if (couponId != null) {
            Coupon coupon = couponService.getCoupon(userId, couponId);
            discountPrice = coupon.calculateDiscountAmount(totalPrice);
        }

        // 3. 포인트 차감 적용 (추가된 부분)
        Long pointToUse = request.pointToUse() != null ? request.pointToUse() : 0L;

        // 최종 금액 = 총액 - 쿠폰할인 - 포인트사용
        Long finalPrice = totalPrice - discountPrice - pointToUse;
        
        // 4. 유효성 검증
        if (finalPrice < 0) {
            throw new BusinessException(ErrorCode.FINAL_PRICE_THAN_ZERO);
        }
        
        log.debug("가격 계산 완료: total={}, discount={}, point={}, final={}", 
            totalPrice, discountPrice, pointToUse, finalPrice);
        
        return OrderPrice.builder()
            .totalPrice(totalPrice)
            .discountPrice(discountPrice)
            .couponId(couponId)
            .pointToUse(pointToUse)
            .finalPrice(finalPrice)
            .build();
    }
    
    private Long calculateTotalPrice(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
            .mapToLong(detail -> detail.unitPrice() * detail.quantity())
            .sum();
    }
}