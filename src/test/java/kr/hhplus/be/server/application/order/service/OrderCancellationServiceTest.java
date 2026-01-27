package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCancellationService 주문 취소 보상 트랜잭션 TDD")
public class OrderCancellationServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderDetailService orderDetailService;

    @Mock
    private PointService pointService;

    @Mock
    private CouponService couponService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderCancellationService orderCancellationService;

    private Order completedOrder;
    private Order completedOrderWithCoupon;
    private Order canceledOrder;
    private OrderDetail orderDetail1;
    private OrderDetail orderDetail2;
    private List<OrderDetail> orderDetails;

    @BeforeEach
    void setUp() {
        // Given: 완료된 주문 (쿠폰 미사용)
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

        // Given: 완료된 주문 (쿠폰 사용)
        completedOrderWithCoupon = new Order(
            2L,
            100L,
            200L,
            50000L,
            5000L,
            45000L,
            OrderStatus.COMPLETED,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // Given: 취소된 주문
        canceledOrder = new Order(
            1L,
            100L,
            null,
            50000L,
            0L,
            50000L,
            OrderStatus.CANCELED,
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
    }

    // ==================== 주문 취소 성공 테스트 ====================

    @Test
    @DisplayName("성공: 쿠폰 미사용 주문 취소 - 포인트, 재고, 판매량 복구")
    void 쿠폰_미사용_주문_취소_성공() {
        // Given
        Long orderId = 1L;

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(orderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 주문 조회 및 취소
        verify(orderService, times(1)).getOrder(orderId);
        verify(orderService, times(1)).cancelOrder(orderId);

        // Then: 주문 상세 조회
        verify(orderDetailService, times(1)).getOrderDetails(orderId);

        // Then: 포인트 환불
        verify(pointService, times(1)).refundPoint(
            eq(100L),
            eq(50000L),
            eq("주문 취소")
        );

        // Then: 쿠폰 복구 호출 안됨
        verify(couponService, never()).restoreCoupon(anyLong(), anyLong());

        // Then: 재고 복구
        verify(productService, times(1)).increaseStock(10L, 2);
        verify(productService, times(1)).increaseStock(20L, 1);

        // Then: 판매량 감소
        verify(productService, times(1)).decreaseSalesQuantity(10L, 2);
        verify(productService, times(1)).decreaseSalesQuantity(20L, 1);
    }

    @Test
    @DisplayName("성공: 쿠폰 사용 주문 취소 - 포인트, 쿠폰, 재고, 판매량 복구")
    void 쿠폰_사용_주문_취소_성공() {
        // Given
        Long orderId = 2L;

        when(orderService.getOrder(orderId)).thenReturn(completedOrderWithCoupon);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(orderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(
            new Order(
                2L,
                100L,
                200L,
                50000L,
                5000L,
                45000L,
                OrderStatus.CANCELED,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        );

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 포인트 환불
        verify(pointService, times(1)).refundPoint(
            eq(100L),
            eq(45000L),
            eq("주문 취소")
        );

        // Then: 쿠폰 복구 호출됨
        verify(couponService, times(1)).restoreCoupon(100L, 200L);

        // Then: 재고 복구
        verify(productService, times(1)).increaseStock(10L, 2);
        verify(productService, times(1)).increaseStock(20L, 1);

        // Then: 판매량 감소
        verify(productService, times(1)).decreaseSalesQuantity(10L, 2);
        verify(productService, times(1)).decreaseSalesQuantity(20L, 1);
    }

    @Test
    @DisplayName("성공: 단일 상품 주문 취소")
    void 단일_상품_주문_취소() {
        // Given
        Long orderId = 1L;
        List<OrderDetail> singleDetail = List.of(orderDetail1);

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(singleDetail);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 재고 복구 1번만 호출
        verify(productService, times(1)).increaseStock(10L, 2);
        verify(productService, times(1)).decreaseSalesQuantity(10L, 2);
    }

    @Test
    @DisplayName("성공: 여러 상품 주문 취소 - 모든 상품 재고 복구")
    void 여러_상품_주문_취소() {
        // Given
        Long orderId = 1L;

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(orderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 각 상품별 재고 복구
        verify(productService, times(2)).increaseStock(anyLong(), anyInt());
        verify(productService, times(2)).decreaseSalesQuantity(anyLong(), anyInt());
    }

    // ==================== 주문 취소 가능 여부 테스트 ====================

    @Test
    @DisplayName("성공: COMPLETED 상태 주문은 취소 가능")
    void 완료_상태_주문_취소_가능() {
        // Given
        Long orderId = 1L;
        when(orderService.getOrder(orderId)).thenReturn(completedOrder);

        // When
        boolean canCancel = orderCancellationService.canCancel(orderId);

        // Then
        assertThat(canCancel).isTrue();
        verify(orderService, times(1)).getOrder(orderId);
    }

    @Test
    @DisplayName("성공: PENDING 상태 주문은 취소 가능")
    void 대기_상태_주문_취소_가능() {
        // Given
        Long orderId = 1L;
        Order pendingOrder = new Order(
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

        when(orderService.getOrder(orderId)).thenReturn(pendingOrder);

        // When
        boolean canCancel = orderCancellationService.canCancel(orderId);

        // Then
        assertThat(canCancel).isTrue();
    }

    @Test
    @DisplayName("성공: CANCELED 상태 주문은 취소 불가")
    void 취소_상태_주문_취소_불가() {
        // Given
        Long orderId = 1L;
        when(orderService.getOrder(orderId)).thenReturn(canceledOrder);

        // When
        boolean canCancel = orderCancellationService.canCancel(orderId);

        // Then
        assertThat(canCancel).isFalse();
    }

    // ==================== 서비스 호출 순서 검증 테스트 ====================

    @Test
    @DisplayName("성공: 주문 취소 프로세스는 정확한 순서로 실행됨")
    void 주문_취소_프로세스_순서_검증() {
        // Given
        Long orderId = 1L;

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(orderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: InOrder를 사용하여 호출 순서 검증
        var inOrder = inOrder(orderService, orderDetailService, orderService, pointService, productService);

        inOrder.verify(orderService).getOrder(orderId);
        inOrder.verify(orderDetailService).getOrderDetails(orderId);
        inOrder.verify(orderService).cancelOrder(orderId);
        inOrder.verify(pointService).refundPoint(anyLong(), anyLong(), anyString());
        inOrder.verify(productService).increaseStock(anyLong(), anyInt());
    }

    // ==================== 빈 주문 상세 처리 테스트 ====================

    @Test
    @DisplayName("성공: 주문 상세가 빈 경우 - 포인트만 환불되고 재고/판매량 처리는 스킵")
    void 빈_주문_상세_처리() {
        // Given
        Long orderId = 1L;
        List<OrderDetail> emptyDetails = List.of();

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(emptyDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 포인트 환불은 실행됨
        verify(pointService, times(1)).refundPoint(anyLong(), anyLong(), anyString());

        // Then: 재고/판매량 처리는 호출되지 않음
        verify(productService, never()).increaseStock(anyLong(), anyInt());
        verify(productService, never()).decreaseSalesQuantity(anyLong(), anyInt());
    }

    // ==================== 대량 주문 취소 테스트 ====================

    @Test
    @DisplayName("성공: 10개 상품 대량 주문 취소")
    void 대량_주문_취소() {
        // Given
        Long orderId = 1L;
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

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(largeOrderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 10개 상품 모두 재고 복구
        verify(productService, times(10)).increaseStock(anyLong(), anyInt());
        verify(productService, times(10)).decreaseSalesQuantity(anyLong(), anyInt());
    }

    // ==================== 포인트 환불 검증 테스트 ====================

    @Test
    @DisplayName("성공: 포인트 환불은 주문의 최종 결제 금액으로 실행됨")
    void 포인트_환불_금액_검증() {
        // Given
        Long orderId = 1L;

        when(orderService.getOrder(orderId)).thenReturn(completedOrder);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(orderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(canceledOrder);

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: finalPrice(50000L)로 환불
        verify(pointService, times(1)).refundPoint(
            eq(100L),
            eq(50000L),
            eq("주문 취소")
        );
    }

    @Test
    @DisplayName("성공: 쿠폰 사용 주문 취소 시 할인된 금액으로 환불")
    void 쿠폰_사용_주문_환불_금액_검증() {
        // Given
        Long orderId = 2L;

        when(orderService.getOrder(orderId)).thenReturn(completedOrderWithCoupon);
        when(orderDetailService.getOrderDetails(orderId)).thenReturn(orderDetails);
        when(orderService.cancelOrder(orderId)).thenReturn(
            new Order(
                2L,
                100L,
                200L,
                50000L,
                5000L,
                45000L,
                OrderStatus.CANCELED,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        );

        // When
        orderCancellationService.cancelOrder(orderId);

        // Then: 할인된 finalPrice(45000L)로 환불
        verify(pointService, times(1)).refundPoint(
            eq(100L),
            eq(45000L),
            eq("주문 취소")
        );
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
