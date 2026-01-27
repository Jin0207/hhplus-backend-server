package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderCompletionService;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.support.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCompletionService 주문 완료 처리 TDD")
public class OrderCompletionServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ProductService productService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderCompletionService orderCompletionService;

    private Order pendingOrder;
    private Order completedOrder;
    private Payment pendingPayment;
    private Payment completedPayment;
    private OrderDetail orderDetail1;
    private OrderDetail orderDetail2;
    private List<OrderDetail> orderDetails;
    private OrderAndPayment orderData;
    private PaymentResult successPaymentResult;

    @BeforeEach
    void setUp() {
        // Given: 대기 중인 주문
        pendingOrder = new Order(
            1L,
            100L,
            null,
            50000L,
            0L,
            50000L,
            OrderStatus.PENDING,
            LocalDateTime.now(),
            null
        );

        // Given: 완료된 주문
        completedOrder = new Order(
            1L,
            100L,
            null,
            50000L,
            0L,
            50000L,
            OrderStatus.COMPLETED,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // Given: 대기 중인 결제
        pendingPayment = Payment.create(
            1L,
            100L,
            "idempotency-key-123",
            50000L,
            PaymentType.POINT
        );
        pendingPayment = new Payment(
            1L,
            1L,
            100L,
            "idempotency-key-123",
            50000L,
            PaymentStatus.PENDING,
            PaymentType.POINT,
            null,
            null,
            null,
            LocalDateTime.now(),
            null,
            false,
            null,
            LocalDateTime.now(),
            null
        );

        // Given: 완료된 결제
        completedPayment = new Payment(
            1L,
            1L,
            100L,
            "idempotency-key-123",
            50000L,
            PaymentStatus.COMPLETED,
            PaymentType.POINT,
            "payment-gateway",
            "tx-12345",
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            false,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // Given: 주문 상세
        orderDetail1 = new OrderDetail(
            1L,
            1L,
            10L,
            2,
            15000L,
            30000L,
            LocalDateTime.now(),
            null
        );

        orderDetail2 = new OrderDetail(
            2L,
            1L,
            20L,
            1,
            20000L,
            20000L,
            LocalDateTime.now(),
            null
        );

        orderDetails = List.of(orderDetail1, orderDetail2);

        // Given: 주문 및 결제 데이터
        orderData = OrderAndPayment.builder()
            .order(pendingOrder)
            .payment(pendingPayment)
            .orderDetails(orderDetails)
            .build();

        // Given: 성공한 결제 결과
        successPaymentResult = PaymentResult.success("tx-12345");
    }

    // ==================== 주문 완료 성공 테스트 ====================

    @Test
    @DisplayName("성공: 주문 완료 처리 - 결제 완료, 주문 완료, 판매량 증가")
    void 주문_완료_처리_성공() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        OrderResponse response = orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then: 결제 완료 호출
        verify(paymentService, times(1)).completePayment(1L, "tx-12345");

        // Then: 주문 완료 호출
        verify(orderService, times(1)).completeOrder(1L);

        // Then: 판매량 증가 호출 (2개 상품)
        verify(productService, times(1)).increaseSalesQuantity(10L, 2);
        verify(productService, times(1)).increaseSalesQuantity(20L, 1);

        // Then: 이벤트 발행
        verify(eventPublisher, times(1)).publishEvent(orderData);

        // Then: 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.orderStatus()).isEqualTo("COMPLETED");
        assertThat(response.payment().status()).isEqualTo("COMPLETED");
        assertThat(response.payment().transactionId()).isEqualTo("tx-12345");
    }

    @Test
    @DisplayName("성공: 단일 상품 주문 완료")
    void 단일_상품_주문_완료() {
        // Given
        List<OrderDetail> singleDetail = List.of(orderDetail1);
        OrderAndPayment singleOrderData = OrderAndPayment.builder()
            .order(pendingOrder)
            .payment(pendingPayment)
            .orderDetails(singleDetail)
            .build();

        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(singleOrderData, successPaymentResult);

        // Then: 판매량 증가 1번만 호출
        verify(productService, times(1)).increaseSalesQuantity(10L, 2);
        verify(productService, times(1)).increaseSalesQuantity(anyLong(), anyInt());
    }

    @Test
    @DisplayName("성공: 여러 상품 주문 완료 - 모든 상품 판매량 증가")
    void 여러_상품_주문_완료() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then: 각 상품별 판매량 증가
        verify(productService, times(2)).increaseSalesQuantity(anyLong(), anyInt());
    }

    @Test
    @DisplayName("성공: 대량 주문 완료 - 10개 상품")
    void 대량_주문_완료() {
        // Given
        List<OrderDetail> largeOrderDetails = List.of(
            createOrderDetail(1L, 1L, 1),
            createOrderDetail(2L, 2L, 2),
            createOrderDetail(3L, 3L, 3),
            createOrderDetail(4L, 4L, 4),
            createOrderDetail(5L, 5L, 5),
            createOrderDetail(6L, 6L, 6),
            createOrderDetail(7L, 7L, 7),
            createOrderDetail(8L, 8L, 8),
            createOrderDetail(9L, 9L, 9),
            createOrderDetail(10L, 10L, 10)
        );

        OrderAndPayment largeOrderData = OrderAndPayment.builder()
            .order(pendingOrder)
            .payment(pendingPayment)
            .orderDetails(largeOrderDetails)
            .build();

        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(largeOrderData, successPaymentResult);

        // Then: 10개 상품 모두 판매량 증가
        verify(productService, times(10)).increaseSalesQuantity(anyLong(), anyInt());
    }

    // ==================== 주문 완료 프로세스 순서 검증 ====================

    @Test
    @DisplayName("성공: 주문 완료 프로세스는 정확한 순서로 실행됨")
    void 주문_완료_프로세스_순서_검증() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then: InOrder를 사용하여 호출 순서 검증
        var inOrder = inOrder(paymentService, orderService, productService, eventPublisher);

        inOrder.verify(paymentService).completePayment(1L, "tx-12345");
        inOrder.verify(orderService).completeOrder(1L);
        inOrder.verify(productService).increaseSalesQuantity(10L, 2);
        inOrder.verify(productService).increaseSalesQuantity(20L, 1);
        inOrder.verify(eventPublisher).publishEvent(any(OrderAndPayment.class));
    }

    // ==================== 결제 완료 검증 ====================

    @Test
    @DisplayName("성공: 결제 완료 시 transactionId가 정확히 전달됨")
    void 결제_완료_transactionId_검증() {
        // Given
        String expectedTransactionId = "tx-12345";
        when(paymentService.completePayment(1L, expectedTransactionId)).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then
        verify(paymentService, times(1)).completePayment(eq(1L), eq(expectedTransactionId));
    }

    @Test
    @DisplayName("성공: 다른 transactionId로 결제 완료")
    void 다른_transactionId_결제_완료() {
        // Given
        String differentTransactionId = "tx-99999";
        PaymentResult differentResult = PaymentResult.success(differentTransactionId);

        Payment differentCompletedPayment = new Payment(
            1L, 1L, 100L, "idempotency-key-123", 50000L,
            PaymentStatus.COMPLETED, PaymentType.POINT,
            "payment-gateway", differentTransactionId, null,
            LocalDateTime.now(), LocalDateTime.now(),
            false, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        when(paymentService.completePayment(1L, differentTransactionId)).thenReturn(differentCompletedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        OrderResponse response = orderCompletionService.completeOrder(orderData, differentResult);

        // Then
        verify(paymentService, times(1)).completePayment(1L, differentTransactionId);
        assertThat(response.payment().transactionId()).isEqualTo(differentTransactionId);
    }

    // ==================== 판매량 증가 검증 ====================

    @Test
    @DisplayName("성공: 각 상품의 판매량이 주문 수량만큼 증가")
    void 판매량_증가_수량_검증() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then: 첫 번째 상품 - productId: 10, quantity: 2
        verify(productService, times(1)).increaseSalesQuantity(eq(10L), eq(2));

        // Then: 두 번째 상품 - productId: 20, quantity: 1
        verify(productService, times(1)).increaseSalesQuantity(eq(20L), eq(1));
    }

    // ==================== 이벤트 발행 검증 ====================

    @Test
    @DisplayName("성공: 주문 완료 후 이벤트가 발행됨")
    void 주문_완료_이벤트_발행() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then: 이벤트 발행 확인
        verify(eventPublisher, times(1)).publishEvent(eq(orderData));
    }

    // ==================== 예외 처리 테스트 ====================

    @Test
    @DisplayName("실패: 결제 완료 중 예외 발생 시 BusinessException 발생")
    void 결제_완료_실패_예외_처리() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345"))
            .thenThrow(new RuntimeException("결제 완료 실패"));

        // When & Then
        assertThatThrownBy(() ->
            orderCompletionService.completeOrder(orderData, successPaymentResult)
        )
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("완료처리");

        // Then: 주문 완료는 호출되지 않음
        verify(orderService, never()).completeOrder(anyLong());

        // Then: 판매량 증가는 호출되지 않음
        verify(productService, never()).increaseSalesQuantity(anyLong(), anyInt());

        // Then: 이벤트는 발행되지 않음
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("실패: 주문 완료 중 예외 발생 시 BusinessException 발생")
    void 주문_완료_실패_예외_처리() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L))
            .thenThrow(new RuntimeException("주문 완료 실패"));

        // When & Then
        assertThatThrownBy(() ->
            orderCompletionService.completeOrder(orderData, successPaymentResult)
        )
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("완료처리");

        // Then: 판매량 증가는 호출되지 않음
        verify(productService, never()).increaseSalesQuantity(anyLong(), anyInt());

        // Then: 이벤트는 발행되지 않음
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("실패: 판매량 증가 중 예외 발생 시 BusinessException 발생")
    void 판매량_증가_실패_예외_처리() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);
        doThrow(new RuntimeException("판매량 증가 실패"))
            .when(productService).increaseSalesQuantity(anyLong(), anyInt());

        // When & Then
        assertThatThrownBy(() ->
            orderCompletionService.completeOrder(orderData, successPaymentResult)
        )
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("완료처리");

        // Then: 이벤트는 발행되지 않음
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ==================== OrderResponse 검증 ====================

    @Test
    @DisplayName("성공: OrderResponse에 모든 주문 정보가 포함됨")
    void OrderResponse_정보_검증() {
        // Given
        when(paymentService.completePayment(1L, "tx-12345")).thenReturn(completedPayment);
        when(orderService.completeOrder(1L)).thenReturn(completedOrder);

        // When
        OrderResponse response = orderCompletionService.completeOrder(orderData, successPaymentResult);

        // Then: 주문 정보
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.orderStatus()).isEqualTo("COMPLETED");
        assertThat(response.totalPrice()).isEqualTo(50000L);
        assertThat(response.discountPrice()).isEqualTo(0L);
        assertThat(response.finalPrice()).isEqualTo(50000L);

        // Then: 결제 정보
        assertThat(response.payment().paymentId()).isEqualTo(1L);
        assertThat(response.payment().status()).isEqualTo("COMPLETED");
        assertThat(response.payment().paymentType()).isEqualTo(PaymentType.POINT);
        assertThat(response.payment().transactionId()).isEqualTo("tx-12345");

        // Then: 주문 상세 정보
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).productId()).isEqualTo(10L);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(1).productId()).isEqualTo(20L);
        assertThat(response.items().get(1).quantity()).isEqualTo(1);
    }

    // ==================== Helper Methods ====================

    private OrderDetail createOrderDetail(Long detailId, Long productId, Integer quantity) {
        return new OrderDetail(
            detailId,
            1L,
            productId,
            quantity,
            10000L,
            quantity * 10000L,
            LocalDateTime.now(),
            null
        );
    }
}
