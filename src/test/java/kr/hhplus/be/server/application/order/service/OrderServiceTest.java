package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.application.product.service.StockService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderDetailRepository orderDetailRepository;
    
    @Mock
    private StockService stockService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    @DisplayName("성공: 주문 생성에 성공한다")
    void 주문_생성_성공() {
        // given
        Long userId = 1L;
        OrderPrice orderPrice = OrderPrice.builder()
            .couponId(1L)
            .totalPrice(20000L)
            .discountPrice(2000L)
            .pointToUse(18000L)
            .finalPrice(18000L)
            .build();
        
        Order savedOrder = new Order(
            1L, userId, 1L, 20000L, 2000L, 18000L,
            OrderStatus.PENDING, LocalDateTime.now(), null
        );
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // when
        Order result = orderService.createOrder(userId, orderPrice);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.couponId()).isEqualTo(1L);
        assertThat(result.totalPrice()).isEqualTo(20000L);
        assertThat(result.discountPrice()).isEqualTo(2000L);
        assertThat(result.finalPrice()).isEqualTo(18000L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
        
        verify(orderRepository).save(argThat(order ->
            order.userId().equals(userId) &&
            order.totalPrice().equals(20000L) &&
            order.orderStatus() == OrderStatus.PENDING
        ));
    }
    
    @Test
    @DisplayName("실패: 최종 금액이 음수인 경우 주문 생성 실패")
    void 최종_금액_음수_주문_생성_실패() {
        // given
        Long userId = 1L;
        OrderPrice orderPrice = OrderPrice.builder()
            .couponId(1L)
            .totalPrice(10000L)
            .discountPrice(15000L)
            .pointToUse(0L)
            .finalPrice(-5000L)
            .build();
        
        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId, orderPrice))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_FAILED);
        
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("성공: 주문 조회에 성공한다")
    void 주문_조회_성공() {
        // given
        Long orderId = 1L;
        Order order = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.PENDING, LocalDateTime.now(), null
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        // when
        Order result = orderService.getOrder(orderId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
        
        verify(orderRepository).findById(orderId);
    }
    
    @Test
    @DisplayName("실패: 존재하지 않는 주문 조회 시 예외가 발생한다")
    void 존재하지_않는_주문_조회_예외() {
        // given
        Long orderId = 999L;
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> orderService.getOrder(orderId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }
    
    @Test
    @DisplayName("성공: 주문 완료에 성공한다")
    void 주문_완료_성공() {
        // given
        Long orderId = 1L;
        Order pendingOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.PENDING, LocalDateTime.now(), null
        );
        
        Order completedOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);
        
        // when
        Order result = orderService.completeOrder(orderId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(result.updDttm()).isNotNull();
        
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(argThat(order ->
            order.orderStatus() == OrderStatus.COMPLETED
        ));
    }
    
    @Test
    @DisplayName("실패: PENDING이 아닌 상태의 주문 완료 시 예외가 발생한다")
    void 주문_완료_예외() {
        // given
        Long orderId = 1L;
        Order completedOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));
        
        // when & then
        assertThatThrownBy(() -> orderService.completeOrder(orderId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VALUE);
        
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("성공: PENDING(대기) 상태 주문 취소에 성공한다")
    void 대기_주문_취소_성공() {
        // given
        Long orderId = 1L;
        Order pendingOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.PENDING, LocalDateTime.now(), null
        );
        
        Order canceledOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.CANCELED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(canceledOrder);
        
        // when
        Order result = orderService.cancelOrder(orderId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.updDttm()).isNotNull();
        
        verify(orderRepository).save(argThat(order ->
            order.orderStatus() == OrderStatus.CANCELED
        ));
    }
    
    @Test
    @DisplayName("성공: COMPLETED(완료) 상태 주문 취소에 성공한다")
    void 완료_주문_취소_성공() {
        // given
        Long orderId = 1L;
        Order completedOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        Order canceledOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.CANCELED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(canceledOrder);
        
        // when
        Order result = orderService.cancelOrder(orderId);
        
        // then
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.CANCELED);
    }
    
    @Test
    @DisplayName("실패: CANCELED 상태 주문은 취소할 수 없다")
    void 주문_취소_불가() {
        // given
        Long orderId = 1L;
        Order canceledOrder = new Order(
            1L, 1L, 1L, 20000L, 2000L, 18000L,
            OrderStatus.CANCELED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(canceledOrder));
        
        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VALUE);
        
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("성공: 쿠폰이 없는 주문 생성에 성공한다")
    void 쿠폰_없는_주문_생성_성공() {
        // given
        Long userId = 1L;
        OrderPrice orderPrice = OrderPrice.builder()
            .couponId(null)
            .totalPrice(20000L)
            .discountPrice(0L)
            .pointToUse(20000L)
            .finalPrice(20000L)
            .build();
        
        Order savedOrder = new Order(
            1L, userId, null, 20000L, 0L, 20000L,
            OrderStatus.PENDING, LocalDateTime.now(), null
        );
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // when
        Order result = orderService.createOrder(userId, orderPrice);
        
        // then
        assertThat(result.couponId()).isNull();
        assertThat(result.discountPrice()).isEqualTo(0L);
        assertThat(result.finalPrice()).isEqualTo(20000L);
    }
}