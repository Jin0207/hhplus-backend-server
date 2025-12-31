package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderPriceCalculator;
import kr.hhplus.be.server.application.order.facade.OrderTransactionManager;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.application.product.facade.StockManagerImpl;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTransactionManager 테스트")
class OrderTransactionManagerTest {

    @Mock private OrderService orderService;
    @Mock private OrderDetailService orderDetailService;
    @Mock private PaymentService paymentService;
    @Mock private OrderPriceCalculator priceCalculator;
    @Mock private PointService pointService;
    @Mock private CouponService couponService;
    @Mock private StockManagerImpl stockManager;
    @Mock private ProductService productService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderTransactionManager orderTransactionManager;

    @Test
    @DisplayName("주문 초기화가 성공한다")
    void 주문_초기화_성공() {
        // given
        Long userId = 1L;
        User user = createUser();
        OrderCreateRequest request = createOrderRequest();
        List<OrderDetail> orderDetails = createOrderDetails();
        OrderPrice orderPrice = createOrderPrice();
        Order order = createOrder();
        Payment payment = createPayment();

        // Stubbing
        when(stockManager.reserveStock(any())).thenReturn(orderDetails);
        when(priceCalculator.calculate(eq(userId), anyList(), any())).thenReturn(orderPrice);
        doNothing().when(couponService).useCoupon(userId, orderPrice.couponId());
        when(pointService.usePoint(eq(userId), eq(orderPrice.finalPrice()), anyString())).thenReturn(user);
        when(orderService.createOrder(userId, orderPrice)).thenReturn(order);
        when(orderDetailService.saveOrderDetails(anyList())).thenReturn(orderDetails);
        doNothing().when(stockManager).recordStockOut(eq(order.id()), anyList(), anyString());
        when(paymentService.createPayment(any(), any(), any(), any(), any())).thenReturn(payment);

        // when
        OrderAndPayment result = orderTransactionManager.initializeOrder(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.order().id()).isEqualTo(order.id());
        
        // 호출 순서 및 인자 검증
        verify(stockManager).reserveStock(request.items());
        verify(pointService).usePoint(userId, orderPrice.finalPrice(), "주문 결제");
        verify(orderDetailService).saveOrderDetails(anyList());
    }

    @Test
    @DisplayName("재고 예약 실패 시 이후 프로세스가 실행되지 않는다")
    void 재고_예약_실패_예외() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        // 괄호 누락 수정 완료
        when(stockManager.reserveStock(any()))
            .thenThrow(new BusinessException(ErrorCode.ORDER_STOCK_INSUFFICIENT));

        // when & then
        assertThatThrownBy(() -> orderTransactionManager.initializeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STOCK_INSUFFICIENT);

        // 검증: 재고 실패 시 가격 계산이나 포인트 차감이 절대 호출되면 안됨
        verify(priceCalculator, never()).calculate(any(), any(), any());
        verify(pointService, never()).usePoint(any(), any(), any());
    }

    @Test
    @DisplayName("결제 완료 처리 중 예외 발생 시 ORDER_FAILED 에러코드로 래핑되어 던져진다")
    void 결제_완료_실패_예외() {
        // given
        OrderAndPayment orderData = createOrderAndPayment();
        PaymentResult paymentResult = PaymentResult.success("tx-123");

        // 하위 서비스에서 예외 발생 시뮬레이션
        when(paymentService.completePayment(any(), any()))
            .thenThrow(new RuntimeException("DB Error"));

        // when & then
        // OrderTransactionManager 구현부의 catch 블록에서 ORDER_FAILED로 래핑함
        assertThatThrownBy(() -> orderTransactionManager.completeOrder(orderData, paymentResult))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_FAILED);

        verify(orderService, never()).completeOrder(any());
    }

    @Test
    @DisplayName("판매량 증가 시 상세 내역의 상품별로 각각 호출된다")
    void 판매량_증가_검증() {
        // given
        OrderAndPayment orderData = createOrderAndPayment();
        PaymentResult paymentResult = PaymentResult.success("tx-123");
        
        when(paymentService.completePayment(any(), any())).thenReturn(createPayment());
        when(orderService.completeOrder(any())).thenReturn(createOrder());

        // when
        orderTransactionManager.completeOrder(orderData, paymentResult);

        // then: 리스트의 각 상품(1L-2개, 2L-1개)에 대해 호출되었는지 확인
        verify(productService).increaseSalesQuantity(1L, 2);
        verify(productService).increaseSalesQuantity(2L, 1);
        verify(productService, times(2)).increaseSalesQuantity(anyLong(), anyInt());
    }

    // --- Helper Methods (필요한 데이터만 간결하게 유지) ---

    private OrderCreateRequest createOrderRequest() {
        return new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(1L, 2), new OrderCreateRequest.OrderItem(2L, 1)),
            1L, 0L, "POINT", "idempotency-key-123"
        );
    }

    private List<OrderDetail> createOrderDetails() {
        return List.of(
            new OrderDetail(1L, null, 1L, 2, 5000L, 10000L, LocalDateTime.now(), null),
            new OrderDetail(2L, null, 2L, 1, 10000L, 10000L, LocalDateTime.now(), null)
        );
    }

    private OrderPrice createOrderPrice() {
        return OrderPrice.builder()
            .couponId(1L).totalPrice(20000L).discountPrice(2000L).pointToUse(0L).finalPrice(18000L)
            .build();
    }

    private Order createOrder() {
        return new Order(1L, 1L, 1L, 20000L, 2000L, 18000L, OrderStatus.PENDING, LocalDateTime.now(), null);
    }

    private Payment createPayment() {
        return new Payment(1L, 1L, 1L, "key", 18000L, PaymentStatus.PENDING, PaymentType.POINT, 
            null, null, null, LocalDateTime.now(), null, false, null, LocalDateTime.now(), null);
    }

    private User createUser() {
        return new User(1L, "test@test.com", "name", 100000L, LocalDateTime.now(), null);
    }

    private OrderAndPayment createOrderAndPayment() {
        return OrderAndPayment.builder()
            .order(createOrder())
            .payment(createPayment())
            .orderDetails(createOrderDetails())
            .build();
    }
}