package kr.hhplus.be.server.application.stock.service;

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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.application.product.service.StockService;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.enums.StockType;
import kr.hhplus.be.server.domain.product.repository.StockRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 재고 이력 관리 TDD")
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Captor
    private ArgumentCaptor<Stock> stockCaptor;

    private OrderDetail orderDetail1;
    private OrderDetail orderDetail2;
    private List<OrderDetail> orderDetails;

    @BeforeEach
    void setUp() {
        // Given: 주문 상세 데이터
        orderDetail1 = new OrderDetail(
            1L,
            100L,
            1L,
            5,
            10000L,
            50000L,
            LocalDateTime.now(),
            null
        );

        orderDetail2 = new OrderDetail(
            2L,
            100L,
            2L,
            3,
            20000L,
            60000L,
            LocalDateTime.now(),
            null
        );

        orderDetails = List.of(orderDetail1, orderDetail2);
    }

    // ==================== 출고 이력 기록 테스트 ====================

    @Test
    @DisplayName("성공: 단일 상품 출고 이력 기록")
    void 단일_상품_출고_이력_기록() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> singleOrder = List.of(orderDetail1);
        String reason = "주문 출고";

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordOut(orderId, singleOrder, reason);

        // Then
        verify(stockRepository, times(1)).save(stockCaptor.capture());

        Stock savedStock = stockCaptor.getValue();
        assertThat(savedStock.productId()).isEqualTo(1L);
        assertThat(savedStock.quantity()).isEqualTo(5);
        assertThat(savedStock.stockType()).isEqualTo(StockType.OUT);
        assertThat(savedStock.reason()).isEqualTo(reason);
        assertThat(savedStock.crtDttm()).isNotNull();
    }

    @Test
    @DisplayName("성공: 여러 상품 출고 이력 기록")
    void 여러_상품_출고_이력_기록() {
        // Given
        Long orderId = 100L;
        String reason = "주문 출고";

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordOut(orderId, orderDetails, reason);

        // Then
        verify(stockRepository, times(2)).save(stockCaptor.capture());

        List<Stock> savedStocks = stockCaptor.getAllValues();

        // 첫 번째 상품 검증
        assertThat(savedStocks.get(0).productId()).isEqualTo(1L);
        assertThat(savedStocks.get(0).quantity()).isEqualTo(5);
        assertThat(savedStocks.get(0).stockType()).isEqualTo(StockType.OUT);
        assertThat(savedStocks.get(0).reason()).isEqualTo(reason);

        // 두 번째 상품 검증
        assertThat(savedStocks.get(1).productId()).isEqualTo(2L);
        assertThat(savedStocks.get(1).quantity()).isEqualTo(3);
        assertThat(savedStocks.get(1).stockType()).isEqualTo(StockType.OUT);
        assertThat(savedStocks.get(1).reason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("성공: 빈 주문 목록으로 출고 이력 기록 - 아무것도 저장하지 않음")
    void 빈_주문_목록_출고_이력_기록() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> emptyDetails = List.of();
        String reason = "주문 출고";

        // When
        stockService.recordOut(orderId, emptyDetails, reason);

        // Then
        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("성공: 출고 이력의 사유가 정확히 기록됨")
    void 출고_이력_사유_기록() {
        // Given
        Long orderId = 100L;
        String customReason = "대량 주문 출고";
        List<OrderDetail> singleOrder = List.of(orderDetail1);

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordOut(orderId, singleOrder, customReason);

        // Then
        verify(stockRepository, times(1)).save(stockCaptor.capture());
        Stock savedStock = stockCaptor.getValue();
        assertThat(savedStock.reason()).isEqualTo(customReason);
    }

    // ==================== 입고 이력 기록 테스트 ====================

    @Test
    @DisplayName("성공: 단일 상품 입고 이력 기록")
    void 단일_상품_입고_이력_기록() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> singleOrder = List.of(orderDetail1);
        String reason = "주문 취소로 인한 재고 복구";

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordIn(orderId, singleOrder, reason);

        // Then
        verify(stockRepository, times(1)).save(stockCaptor.capture());

        Stock savedStock = stockCaptor.getValue();
        assertThat(savedStock.productId()).isEqualTo(1L);
        assertThat(savedStock.quantity()).isEqualTo(5);
        assertThat(savedStock.stockType()).isEqualTo(StockType.IN);
        assertThat(savedStock.reason()).isEqualTo(reason);
        assertThat(savedStock.crtDttm()).isNotNull();
    }

    @Test
    @DisplayName("성공: 여러 상품 입고 이력 기록")
    void 여러_상품_입고_이력_기록() {
        // Given
        Long orderId = 100L;
        String reason = "주문 취소로 인한 재고 복구";

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordIn(orderId, orderDetails, reason);

        // Then
        verify(stockRepository, times(2)).save(stockCaptor.capture());

        List<Stock> savedStocks = stockCaptor.getAllValues();

        // 첫 번째 상품 검증
        assertThat(savedStocks.get(0).productId()).isEqualTo(1L);
        assertThat(savedStocks.get(0).quantity()).isEqualTo(5);
        assertThat(savedStocks.get(0).stockType()).isEqualTo(StockType.IN);
        assertThat(savedStocks.get(0).reason()).isEqualTo(reason);

        // 두 번째 상품 검증
        assertThat(savedStocks.get(1).productId()).isEqualTo(2L);
        assertThat(savedStocks.get(1).quantity()).isEqualTo(3);
        assertThat(savedStocks.get(1).stockType()).isEqualTo(StockType.IN);
        assertThat(savedStocks.get(1).reason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("성공: 빈 주문 목록으로 입고 이력 기록 - 아무것도 저장하지 않음")
    void 빈_주문_목록_입고_이력_기록() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> emptyDetails = List.of();
        String reason = "주문 취소로 인한 재고 복구";

        // When
        stockService.recordIn(orderId, emptyDetails, reason);

        // Then
        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("성공: 입고 이력의 사유가 정확히 기록됨")
    void 입고_이력_사유_기록() {
        // Given
        Long orderId = 100L;
        String customReason = "반품 처리";
        List<OrderDetail> singleOrder = List.of(orderDetail1);

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordIn(orderId, singleOrder, customReason);

        // Then
        verify(stockRepository, times(1)).save(stockCaptor.capture());
        Stock savedStock = stockCaptor.getValue();
        assertThat(savedStock.reason()).isEqualTo(customReason);
    }

    // ==================== 출고/입고 정확성 비교 테스트 ====================

    @Test
    @DisplayName("성공: 동일 주문에 대한 출고와 입고 이력은 타입만 다르고 나머지는 동일")
    void 출고_입고_이력_타입_차이_검증() {
        // Given
        Long orderId = 100L;
        List<OrderDetail> singleOrder = List.of(orderDetail1);
        String outReason = "주문 출고";
        String inReason = "주문 취소";

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When: 출고 이력 기록
        stockService.recordOut(orderId, singleOrder, outReason);

        // Then: 입고 이력 기록
        stockService.recordIn(orderId, singleOrder, inReason);

        // Verify
        verify(stockRepository, times(2)).save(stockCaptor.capture());

        List<Stock> savedStocks = stockCaptor.getAllValues();
        Stock outStock = savedStocks.get(0);
        Stock inStock = savedStocks.get(1);

        // 타입은 다름
        assertThat(outStock.stockType()).isEqualTo(StockType.OUT);
        assertThat(inStock.stockType()).isEqualTo(StockType.IN);

        // productId와 수량은 동일
        assertThat(outStock.productId()).isEqualTo(inStock.productId());
        assertThat(outStock.quantity()).isEqualTo(inStock.quantity());
    }

    @Test
    @DisplayName("성공: 대량 주문(10개 상품) 출고 이력 기록")
    void 대량_주문_출고_이력_기록() {
        // Given
        Long orderId = 200L;
        String reason = "대량 주문 출고";

        List<OrderDetail> largeOrderDetails = List.of(
            createOrderDetail(1L, 1L, 5),
            createOrderDetail(2L, 2L, 3),
            createOrderDetail(3L, 3L, 10),
            createOrderDetail(4L, 4L, 2),
            createOrderDetail(5L, 5L, 7),
            createOrderDetail(6L, 6L, 1),
            createOrderDetail(7L, 7L, 4),
            createOrderDetail(8L, 8L, 6),
            createOrderDetail(9L, 9L, 8),
            createOrderDetail(10L, 10L, 9)
        );

        when(stockRepository.save(any(Stock.class)))
            .thenAnswer(invocation -> {
                Stock stock = invocation.getArgument(0);
                return new Stock(1L, stock.productId(), stock.quantity(), stock.stockType(), stock.reason(), stock.crtDttm());
            });

        // When
        stockService.recordOut(orderId, largeOrderDetails, reason);

        // Then
        verify(stockRepository, times(10)).save(any(Stock.class));
    }

    // ==================== Helper Methods ====================

    private OrderDetail createOrderDetail(Long detailId, Long productId, Integer quantity) {
        return new OrderDetail(
            detailId,
            100L,
            productId,
            quantity,
            10000L,
            quantity * 10000L,
            LocalDateTime.now(),
            null
        );
    }
}
