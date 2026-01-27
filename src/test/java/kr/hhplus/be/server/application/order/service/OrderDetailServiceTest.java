package kr.hhplus.be.server.application.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderDetailService 주문 상세 관리 TDD")
public class OrderDetailServiceTest {

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @InjectMocks
    private OrderDetailService orderDetailService;

    private OrderDetail orderDetail1;
    private OrderDetail orderDetail2;
    private OrderDetail orderDetail3;
    private List<OrderDetail> orderDetailList;

    @BeforeEach
    void setUp() {
        // Given: 주문 상세 데이터
        orderDetail1 = new OrderDetail(
            1L,
            100L,
            10L,
            2,
            15000L,
            30000L,
            LocalDateTime.now(),
            null
        );

        orderDetail2 = new OrderDetail(
            2L,
            100L,
            20L,
            1,
            20000L,
            20000L,
            LocalDateTime.now(),
            null
        );

        orderDetail3 = new OrderDetail(
            3L,
            100L,
            30L,
            3,
            10000L,
            30000L,
            LocalDateTime.now(),
            null
        );

        orderDetailList = List.of(orderDetail1, orderDetail2, orderDetail3);
    }

    // ==================== 주문 상세 저장 테스트 (복수) ====================

    @Test
    @DisplayName("성공: 여러 주문 상세 저장")
    void saveOrderDetails_성공_여러_상세() {
        // Given
        when(orderDetailRepository.saveAll(orderDetailList)).thenReturn(orderDetailList);

        // When
        List<OrderDetail> result = orderDetailService.saveOrderDetails(orderDetailList);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(2).id()).isEqualTo(3L);
        verify(orderDetailRepository, times(1)).saveAll(orderDetailList);
    }

    @Test
    @DisplayName("성공: 단일 주문 상세 저장 (리스트로)")
    void saveOrderDetails_성공_단일_상세() {
        // Given
        List<OrderDetail> singleDetailList = List.of(orderDetail1);
        when(orderDetailRepository.saveAll(singleDetailList)).thenReturn(singleDetailList);

        // When
        List<OrderDetail> result = orderDetailService.saveOrderDetails(singleDetailList);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).orderId()).isEqualTo(100L);
        assertThat(result.get(0).productId()).isEqualTo(10L);
        assertThat(result.get(0).quantity()).isEqualTo(2);
        verify(orderDetailRepository, times(1)).saveAll(singleDetailList);
    }

    @Test
    @DisplayName("성공: 빈 리스트 저장")
    void saveOrderDetails_성공_빈_리스트() {
        // Given
        List<OrderDetail> emptyList = List.of();
        when(orderDetailRepository.saveAll(emptyList)).thenReturn(emptyList);

        // When
        List<OrderDetail> result = orderDetailService.saveOrderDetails(emptyList);

        // Then
        assertThat(result).isEmpty();
        verify(orderDetailRepository, times(1)).saveAll(emptyList);
    }

    @Test
    @DisplayName("성공: 대량 주문 상세 저장 (10개)")
    void saveOrderDetails_성공_대량_저장() {
        // Given
        List<OrderDetail> largeDetailList = List.of(
            createOrderDetail(1L, 100L, 1L, 1),
            createOrderDetail(2L, 100L, 2L, 2),
            createOrderDetail(3L, 100L, 3L, 3),
            createOrderDetail(4L, 100L, 4L, 4),
            createOrderDetail(5L, 100L, 5L, 5),
            createOrderDetail(6L, 100L, 6L, 6),
            createOrderDetail(7L, 100L, 7L, 7),
            createOrderDetail(8L, 100L, 8L, 8),
            createOrderDetail(9L, 100L, 9L, 9),
            createOrderDetail(10L, 100L, 10L, 10)
        );

        when(orderDetailRepository.saveAll(largeDetailList)).thenReturn(largeDetailList);

        // When
        List<OrderDetail> result = orderDetailService.saveOrderDetails(largeDetailList);

        // Then
        assertThat(result).hasSize(10);
        verify(orderDetailRepository, times(1)).saveAll(largeDetailList);
    }

    @Test
    @DisplayName("성공: 주문 상세 저장 시 데이터 정확성 검증")
    void saveOrderDetails_성공_데이터_정확성() {
        // Given
        ArgumentCaptor<List<OrderDetail>> captor = ArgumentCaptor.forClass(List.class);
        when(orderDetailRepository.saveAll(any(List.class))).thenReturn(orderDetailList);

        // When
        List<OrderDetail> result = orderDetailService.saveOrderDetails(orderDetailList);

        // Then
        verify(orderDetailRepository, times(1)).saveAll(captor.capture());
        List<OrderDetail> capturedList = captor.getValue();

        // 첫 번째 상세 검증
        assertThat(capturedList.get(0).orderId()).isEqualTo(100L);
        assertThat(capturedList.get(0).productId()).isEqualTo(10L);
        assertThat(capturedList.get(0).quantity()).isEqualTo(2);
        assertThat(capturedList.get(0).unitPrice()).isEqualTo(15000L);
        assertThat(capturedList.get(0).subtotal()).isEqualTo(30000L);

        // 두 번째 상세 검증
        assertThat(capturedList.get(1).orderId()).isEqualTo(100L);
        assertThat(capturedList.get(1).productId()).isEqualTo(20L);
        assertThat(capturedList.get(1).quantity()).isEqualTo(1);
        assertThat(capturedList.get(1).unitPrice()).isEqualTo(20000L);
        assertThat(capturedList.get(1).subtotal()).isEqualTo(20000L);
    }

    // ==================== 주문 상세 단건 저장 테스트 ====================

    @Test
    @DisplayName("성공: 주문 상세 단건 저장")
    void saveOrderDetail_성공() {
        // Given
        when(orderDetailRepository.save(orderDetail1)).thenReturn(orderDetail1);

        // When
        OrderDetail result = orderDetailService.saveOrderDetail(orderDetail1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.orderId()).isEqualTo(100L);
        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.unitPrice()).isEqualTo(15000L);
        assertThat(result.subtotal()).isEqualTo(30000L);
        verify(orderDetailRepository, times(1)).save(orderDetail1);
    }

    @Test
    @DisplayName("성공: 다양한 주문 상세 단건 저장")
    void saveOrderDetail_성공_다양한_상세() {
        // Given
        OrderDetail[] orderDetails = {orderDetail1, orderDetail2, orderDetail3};

        for (OrderDetail detail : orderDetails) {
            when(orderDetailRepository.save(detail)).thenReturn(detail);
        }

        // When & Then
        for (OrderDetail detail : orderDetails) {
            OrderDetail result = orderDetailService.saveOrderDetail(detail);
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(detail.id());
            assertThat(result.orderId()).isEqualTo(detail.orderId());
            assertThat(result.productId()).isEqualTo(detail.productId());
        }

        verify(orderDetailRepository, times(3)).save(any(OrderDetail.class));
    }

    @Test
    @DisplayName("성공: 주문 상세 단건 저장 시 데이터 정확성 검증")
    void saveOrderDetail_성공_데이터_정확성() {
        // Given
        ArgumentCaptor<OrderDetail> captor = ArgumentCaptor.forClass(OrderDetail.class);
        when(orderDetailRepository.save(any(OrderDetail.class))).thenReturn(orderDetail1);

        // When
        OrderDetail result = orderDetailService.saveOrderDetail(orderDetail1);

        // Then
        verify(orderDetailRepository, times(1)).save(captor.capture());
        OrderDetail capturedDetail = captor.getValue();

        assertThat(capturedDetail.id()).isEqualTo(1L);
        assertThat(capturedDetail.orderId()).isEqualTo(100L);
        assertThat(capturedDetail.productId()).isEqualTo(10L);
        assertThat(capturedDetail.quantity()).isEqualTo(2);
        assertThat(capturedDetail.unitPrice()).isEqualTo(15000L);
        assertThat(capturedDetail.subtotal()).isEqualTo(30000L);
        assertThat(capturedDetail.crtDttm()).isNotNull();
    }

    // ==================== 주문 상세 조회 테스트 ====================

    @Test
    @DisplayName("성공: 주문 ID로 주문 상세 목록 조회")
    void getOrderDetails_성공() {
        // Given
        Long orderId = 100L;
        when(orderDetailRepository.findByOrderId(orderId)).thenReturn(orderDetailList);

        // When
        List<OrderDetail> result = orderDetailService.getOrderDetails(orderId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).orderId()).isEqualTo(orderId);
        assertThat(result.get(1).orderId()).isEqualTo(orderId);
        assertThat(result.get(2).orderId()).isEqualTo(orderId);
        verify(orderDetailRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("성공: 주문 ID로 주문 상세 조회 - 단건")
    void getOrderDetails_성공_단건() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> singleDetailList = List.of(orderDetail1);
        when(orderDetailRepository.findByOrderId(orderId)).thenReturn(singleDetailList);

        // When
        List<OrderDetail> result = orderDetailService.getOrderDetails(orderId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).orderId()).isEqualTo(orderId);
        assertThat(result.get(0).productId()).isEqualTo(10L);
        verify(orderDetailRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("성공: 주문 ID로 주문 상세 조회 - 빈 결과")
    void getOrderDetails_성공_빈_결과() {
        // Given
        Long orderId = 999L;
        when(orderDetailRepository.findByOrderId(orderId)).thenReturn(List.of());

        // When
        List<OrderDetail> result = orderDetailService.getOrderDetails(orderId);

        // Then
        assertThat(result).isEmpty();
        verify(orderDetailRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("성공: 주문 ID로 주문 상세 조회 - 대량 데이터")
    void getOrderDetails_성공_대량_데이터() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> largeDetailList = List.of(
            createOrderDetail(1L, orderId, 1L, 1),
            createOrderDetail(2L, orderId, 2L, 2),
            createOrderDetail(3L, orderId, 3L, 3),
            createOrderDetail(4L, orderId, 4L, 4),
            createOrderDetail(5L, orderId, 5L, 5),
            createOrderDetail(6L, orderId, 6L, 6),
            createOrderDetail(7L, orderId, 7L, 7),
            createOrderDetail(8L, orderId, 8L, 8),
            createOrderDetail(9L, orderId, 9L, 9),
            createOrderDetail(10L, orderId, 10L, 10)
        );

        when(orderDetailRepository.findByOrderId(orderId)).thenReturn(largeDetailList);

        // When
        List<OrderDetail> result = orderDetailService.getOrderDetails(orderId);

        // Then
        assertThat(result).hasSize(10);
        assertThat(result).allMatch(detail -> detail.orderId().equals(orderId));
        verify(orderDetailRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("성공: 다른 주문 ID로 조회 시 각각 다른 결과")
    void getOrderDetails_성공_다른_주문_ID() {
        // Given
        Long orderId1 = 100L;
        Long orderId2 = 200L;

        List<OrderDetail> order1Details = List.of(orderDetail1, orderDetail2);
        List<OrderDetail> order2Details = List.of(orderDetail3);

        when(orderDetailRepository.findByOrderId(orderId1)).thenReturn(order1Details);
        when(orderDetailRepository.findByOrderId(orderId2)).thenReturn(order2Details);

        // When
        List<OrderDetail> result1 = orderDetailService.getOrderDetails(orderId1);
        List<OrderDetail> result2 = orderDetailService.getOrderDetails(orderId2);

        // Then
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(1);
        verify(orderDetailRepository, times(1)).findByOrderId(orderId1);
        verify(orderDetailRepository, times(1)).findByOrderId(orderId2);
    }

    @Test
    @DisplayName("성공: 주문 상세 조회 결과의 데이터 정확성 검증")
    void getOrderDetails_성공_데이터_정확성() {
        // Given
        Long orderId = 100L;
        when(orderDetailRepository.findByOrderId(orderId)).thenReturn(orderDetailList);

        // When
        List<OrderDetail> result = orderDetailService.getOrderDetails(orderId);

        // Then
        // 첫 번째 상세 검증
        OrderDetail first = result.get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.orderId()).isEqualTo(100L);
        assertThat(first.productId()).isEqualTo(10L);
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.unitPrice()).isEqualTo(15000L);
        assertThat(first.subtotal()).isEqualTo(30000L);

        // 두 번째 상세 검증
        OrderDetail second = result.get(1);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(second.orderId()).isEqualTo(100L);
        assertThat(second.productId()).isEqualTo(20L);
        assertThat(second.quantity()).isEqualTo(1);
        assertThat(second.unitPrice()).isEqualTo(20000L);
        assertThat(second.subtotal()).isEqualTo(20000L);

        // 세 번째 상세 검증
        OrderDetail third = result.get(2);
        assertThat(third.id()).isEqualTo(3L);
        assertThat(third.orderId()).isEqualTo(100L);
        assertThat(third.productId()).isEqualTo(30L);
        assertThat(third.quantity()).isEqualTo(3);
        assertThat(third.unitPrice()).isEqualTo(10000L);
        assertThat(third.subtotal()).isEqualTo(30000L);
    }

    // ==================== Helper Methods ====================

    private OrderDetail createOrderDetail(Long detailId, Long orderId, Long productId, Integer quantity) {
        return new OrderDetail(
            detailId,
            orderId,
            productId,
            quantity,
            10000L,
            quantity * 10000L,
            LocalDateTime.now(),
            null
        );
    }
}
