package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.order.facade.OrderTransactionManager;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.application.payment.facade.PaymentProcessorImpl;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFacade 테스트")
class OrderFacadeTest {
    
    @Mock
    private OrderTransactionManager orderTransactionManager;
    
    @Mock
    private PaymentProcessorImpl paymentProcessor;
    
    @InjectMocks
    private OrderFacade orderFacade;
    
    @Test
    @DisplayName("성공: 주문 생성을 성공한다")
    void 주문_생성_성공() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        OrderAndPayment orderData = createOrderAndPayment();
        PaymentResult paymentResult = PaymentResult.success("tx-123");
        OrderResponse expectedResponse = createOrderResponse();
        
        doNothing().when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        when(orderTransactionManager.initializeOrder(userId, request)).thenReturn(orderData);
        when(paymentProcessor.processPayment(orderData.payment(), request)).thenReturn(paymentResult);
        when(orderTransactionManager.completeOrder(orderData, paymentResult)).thenReturn(expectedResponse);
        
        // when
        OrderResponse response = orderFacade.completeOrder(userId, request);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.COMPLETED.name());
        assertThat(response.totalPrice()).isEqualTo(20000L);
        assertThat(response.discountPrice()).isEqualTo(2000L);
        assertThat(response.finalPrice()).isEqualTo(18000L);
        assertThat(response.payment()).isNotNull();
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.COMPLETED.name());
        assertThat(response.items()).hasSize(2);
        
        verify(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        verify(orderTransactionManager).initializeOrder(userId, request);
        verify(paymentProcessor).processPayment(orderData.payment(), request);
        verify(orderTransactionManager).completeOrder(orderData, paymentResult);
    }
    
    @Test
    @DisplayName("실패: 멱등성 키 중복 시 예외가 발생한다")
    void 멱등성_키_중복_예외() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        doThrow(new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST))
            .when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        
        verify(orderTransactionManager, never()).initializeOrder(any(), any());
        verify(paymentProcessor, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("실패: 재고 부족 시 트랜잭션이 롤백된다")
    void 재고_부족_트랜잭션_롤백() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        // when
        when(orderTransactionManager.initializeOrder(userId, request))
            .thenThrow(new BusinessException(ErrorCode.ORDER_STOCK_INSUFFICIENT));
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STOCK_INSUFFICIENT);
        
        verify(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        verify(paymentProcessor, never()).processPayment(any(), any());
        verify(orderTransactionManager, never()).completeOrder(any(), any());
    }
    
    @Test
    @DisplayName("실패:포인트 부족 시 트랜잭션이 롤백된다")
    void 포인트_부족_트랜잭션_롤백() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        doNothing().when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        when(orderTransactionManager.initializeOrder(userId, request))
            .thenThrow(new BusinessException(ErrorCode.POINT_BALANCE_INSUFFICIENT));
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_BALANCE_INSUFFICIENT);
        
        verify(paymentProcessor, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("실패: 결제 실패 시 트랜잭션이 롤백된다")
    void 결제_실패_트랜잭션_롤백() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        OrderAndPayment orderData = createOrderAndPayment();
        PaymentResult paymentResult = PaymentResult.fail("결제 한도 초과");
        
        doNothing().when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        when(orderTransactionManager.initializeOrder(userId, request)).thenReturn(orderData);
        when(paymentProcessor.processPayment(orderData.payment(), request)).thenReturn(paymentResult);
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_FAILED)
            .hasMessageContaining("결제 한도 초과");
        
        verify(orderTransactionManager, never()).completeOrder(any(), any());
    }
    
    @Test
    @DisplayName("실패: 쿠폰이 이미 사용된 경우 트랜잭션이 롤백된다")
    void 쿠폰_이미_사용됨_트랜잭션_롤백() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        doNothing().when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        when(orderTransactionManager.initializeOrder(userId, request))
            .thenThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
    }
    
    @Test
    @DisplayName("실패: 주문 완료 처리 실패 시 트랜잭션이 롤백된다")
    void 주문_완료_처리_실패_트랜잭션_롤백() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        OrderAndPayment orderData = createOrderAndPayment();
        PaymentResult paymentResult = PaymentResult.success("tx-123");
        
        doNothing().when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        when(orderTransactionManager.initializeOrder(userId, request)).thenReturn(orderData);
        when(paymentProcessor.processPayment(orderData.payment(), request)).thenReturn(paymentResult);
        when(orderTransactionManager.completeOrder(orderData, paymentResult))
            .thenThrow(new BusinessException(ErrorCode.ORDER_FAILED, "완료처리"));
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_FAILED);
    }
    
    @Test
    @DisplayName("실패: 시스템 예외 발생 시 BusinessException으로 변환된다")
    void 시스템_예외_변환() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = createOrderRequest();
        
        doNothing().when(paymentProcessor).validateIdempotencyKey(request.idempotencyKey());
        when(orderTransactionManager.initializeOrder(userId, request))
            .thenThrow(new RuntimeException("예상치 못한 오류"));
        
        // when & then
        assertThatThrownBy(() -> orderFacade.completeOrder(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_FAILED);
    }
    
    private OrderCreateRequest createOrderRequest() {
        List<OrderCreateRequest.OrderItem> items = List.of(
            new OrderCreateRequest.OrderItem(1L, 2),
            new OrderCreateRequest.OrderItem(2L, 1)
        );
        
        return new OrderCreateRequest(
            items,
            1L, // couponId
            0L, // pointToUse
            PaymentType.POINT.name(),
            "idempotency-key-123"
        );
    }
    
    private OrderAndPayment createOrderAndPayment() {
        Order order = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.PENDING, LocalDateTime.now(), null
        );
        
        Payment payment = new Payment(
            1L, 1L, 1L, "idempotency-key-123",
            18000L, PaymentStatus.PENDING, PaymentType.POINT,
            null, null, null, LocalDateTime.now(), null,
            false, null, LocalDateTime.now(), null
        );
        
        List<OrderDetail> details = List.of(
            new OrderDetail(1L, 1L, 1L, 2, 5000L, 10000L, LocalDateTime.now(), null),
            new OrderDetail(2L, 1L, 2L, 1, 10000L, 10000L, LocalDateTime.now(), null)
        );
        
        return OrderAndPayment.builder()
            .order(order)
            .payment(payment)
            .orderDetails(details)
            .build();
    }
    
    private OrderResponse createOrderResponse() {
        OrderResponse.PaymentInfo paymentInfo = OrderResponse.PaymentInfo.builder()
            .paymentId(1L)
            .status(PaymentStatus.COMPLETED.name())
            .paymentType(PaymentType.POINT)
            .transactionId("tx-123")
            .build();
        
        List<OrderResponse.OrderItemInfo> items = List.of(
            OrderResponse.OrderItemInfo.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(5000L)
                .subtotal(10000L)
                .build(),
            OrderResponse.OrderItemInfo.builder()
                .productId(2L)
                .quantity(1)
                .unitPrice(10000L)
                .subtotal(10000L)
                .build()
        );
        
        return OrderResponse.builder()
            .orderId(1L)
            .userId(1L)
            .orderStatus(OrderStatus.COMPLETED.name())
            .totalPrice(20000L)
            .discountPrice(2000L)
            .finalPrice(18000L)
            .payment(paymentInfo)
            .items(items)
            .createdAt(LocalDateTime.now())
            .build();
    }
}