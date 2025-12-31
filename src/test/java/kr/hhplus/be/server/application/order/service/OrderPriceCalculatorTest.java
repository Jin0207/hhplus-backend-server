package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.application.order.facade.OrderPriceCalculator;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPriceCalculator 단위 테스트")
class OrderPriceCalculatorTest {

    @InjectMocks
    private OrderPriceCalculator orderPriceCalculator;

    @Mock
    private CouponService couponService;

    @Test
    @DisplayName("성공: 쿠폰과 포인트를 모두 적용했을 때 금액 계산이 정확해야 한다")
    void calculate_Success_WhenCouponAndPointApplied() {
        // 1. Given (준비)
        Long userId = 1L;
        Long couponId = 100L;
        Long itemPrice = 15000L;
        Integer quantity = 2;
        Long totalAmount = itemPrice * quantity; // 30,000L
        Long pointToUse = 5000L;
        Long couponDiscount = 3000L;

        // 주문 상세 리스트 생성
        OrderDetail detail = OrderDetail.create(null, 1L, quantity, itemPrice);
        List<OrderDetail> orderDetails = List.of(detail);

        // 요청 객체 생성 (포인트 5,000원 사용)
        OrderCreateRequest request = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(1L, quantity)),
            couponId,
            pointToUse,
            "POINT",
            "test-idempotency-key"
        );

        // 모킹 설정 (when 스타일)
        Coupon mockCoupon = mock(Coupon.class);
        when(couponService.getCoupon(userId, couponId)).thenReturn(mockCoupon);
        when(mockCoupon.calculateDiscountAmount(totalAmount)).thenReturn(couponDiscount);

        // 2. When (실행)
        OrderPrice result = orderPriceCalculator.calculate(userId, orderDetails, request);

        // 3. Then (검증)
        // 기대값: 30,000(총액) - 3,000(쿠폰) - 5,000(포인트) = 22,000
        assertThat(result.totalPrice()).isEqualTo(30_000L);
        assertThat(result.discountPrice()).isEqualTo(3_000L);
        assertThat(result.pointToUse()).isEqualTo(5_000L);
        assertThat(result.finalPrice()).isEqualTo(22_000L);

        // 협력 객체 호출 여부 확인
        verify(couponService, times(1)).getCoupon(userId, couponId);
        verify(mockCoupon, times(1)).calculateDiscountAmount(totalAmount);
    }

    @Test
    @DisplayName("실패: 포인트 사용액이 주문 금액보다 커서 최종 결제 금액이 마이너스면 예외가 발생한다")
    void calculate_Fail_WhenFinalPriceIsNegative() {
        // 1. Given
        Long userId = 1L;
        Long itemPrice = 2000L; // 2,000원 주문
        Long excessivePoint = 3000L; // 3,000원 포인트 사용 시도

        OrderDetail detail = OrderDetail.create(null, 1L, 1, itemPrice);
        OrderCreateRequest request = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(1L, 1)),
            null, // 쿠폰 없음
            excessivePoint,
            "POINT",
            "test-idempotency-key-2"
        );

        // 2. When & 3. Then
        assertThatThrownBy(() -> orderPriceCalculator.calculate(userId, List.of(detail), request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FINAL_PRICE_THAN_ZERO);

        // 쿠폰이 없으므로 서비스 호출이 일어나지 않아야 함을 증빙
        verify(couponService, never()).getCoupon(anyLong(), anyLong());
    }
}