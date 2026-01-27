package kr.hhplus.be.server.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.repository.PopularProductRepository;
import kr.hhplus.be.server.infrastructure.product.persistence.PopularProductCustomRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PopularProductScheduler 배치 테스트")
class PopularProductSchedulerTest {

    @Mock
    private PopularProductRepository popularProductRepository;

    @Mock
    private PopularProductCustomRepository popularProductCustomRepository;

    @InjectMocks
    private PopularProductScheduler scheduler;

    @Captor
    private ArgumentCaptor<List<PopularProduct>> productListCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> dateCaptor;

    @BeforeEach
    void setUp() {
        // @Value 필드 설정
        ReflectionTestUtils.setField(scheduler, "topCount", 5);
        ReflectionTestUtils.setField(scheduler, "periodDays", 3);
        ReflectionTestUtils.setField(scheduler, "cleanupDays", 7);
    }

    @Nested
    @DisplayName("aggregatePopularProducts 메서드")
    class AggregatePopularProducts {

        @Test
        @DisplayName("성공: 집계 결과가 있으면 기존 데이터 삭제 후 저장한다")
        void 집계_성공_저장() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(3);
            LocalDate endDate = today.minusDays(1);

            List<PopularProduct> aggregatedProducts = List.of(
                PopularProduct.fromAggregation(1, 1L, "상품1", 10000L, ProductCategory.TOP, 100, today),
                PopularProduct.fromAggregation(2, 2L, "상품2", 20000L, ProductCategory.PANTS, 80, today),
                PopularProduct.fromAggregation(3, 3L, "상품3", 15000L, ProductCategory.OUTER, 60, today)
            );

            when(popularProductCustomRepository.aggregateTopSellingProducts(
                eq(startDate), eq(endDate), eq(5), eq(today)
            )).thenReturn(aggregatedProducts);

            when(popularProductRepository.saveAll(any()))
                .thenReturn(aggregatedProducts);

            // when
            scheduler.aggregatePopularProducts();

            // then
            // 1. 기존 데이터 삭제 호출 확인
            verify(popularProductRepository, times(1)).deleteByBaseDate(today);

            // 2. 집계 쿼리 호출 확인
            verify(popularProductCustomRepository, times(1))
                .aggregateTopSellingProducts(startDate, endDate, 5, today);

            // 3. 저장 호출 확인
            verify(popularProductRepository, times(1)).saveAll(productListCaptor.capture());
            List<PopularProduct> savedProducts = productListCaptor.getValue();
            assertEquals(3, savedProducts.size());
            assertEquals("상품1", savedProducts.get(0).productName());
        }

        @Test
        @DisplayName("성공: 집계 결과가 없으면 저장하지 않는다")
        void 집계_결과_없음_저장_안함() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(3);
            LocalDate endDate = today.minusDays(1);

            when(popularProductCustomRepository.aggregateTopSellingProducts(
                eq(startDate), eq(endDate), eq(5), eq(today)
            )).thenReturn(List.of());

            // when
            scheduler.aggregatePopularProducts();

            // then
            verify(popularProductRepository, times(1)).deleteByBaseDate(today);
            verify(popularProductCustomRepository, times(1))
                .aggregateTopSellingProducts(startDate, endDate, 5, today);
            // 빈 결과이므로 saveAll 호출 안됨
            verify(popularProductRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("성공: 멱등성 보장 - 같은 날 여러 번 실행해도 데이터 일관성 유지")
        void 멱등성_보장() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(3);
            LocalDate endDate = today.minusDays(1);

            List<PopularProduct> aggregatedProducts = List.of(
                PopularProduct.fromAggregation(1, 1L, "상품1", 10000L, ProductCategory.TOP, 100, today)
            );

            when(popularProductCustomRepository.aggregateTopSellingProducts(
                any(), any(), anyInt(), any()
            )).thenReturn(aggregatedProducts);

            when(popularProductRepository.saveAll(any()))
                .thenReturn(aggregatedProducts);

            // when - 2번 실행
            scheduler.aggregatePopularProducts();
            scheduler.aggregatePopularProducts();

            // then - 각 실행마다 deleteByBaseDate가 먼저 호출되어 멱등성 보장
            verify(popularProductRepository, times(2)).deleteByBaseDate(today);
            verify(popularProductRepository, times(2)).saveAll(any());
        }

        @Test
        @DisplayName("성공: 집계 기간이 D-3 ~ D-1로 정확히 계산된다")
        void 집계_기간_검증() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate expectedStartDate = today.minusDays(3);
            LocalDate expectedEndDate = today.minusDays(1);

            when(popularProductCustomRepository.aggregateTopSellingProducts(
                any(), any(), anyInt(), any()
            )).thenReturn(List.of());

            // when
            scheduler.aggregatePopularProducts();

            // then
            verify(popularProductCustomRepository, times(1))
                .aggregateTopSellingProducts(
                    eq(expectedStartDate),
                    eq(expectedEndDate),
                    eq(5),
                    eq(today)
                );
        }
    }

    @Nested
    @DisplayName("cleanupOldData 메서드")
    class CleanupOldData {

        @Test
        @DisplayName("성공: 7일 이전 데이터를 삭제한다")
        void 오래된_데이터_삭제() {
            // given
            LocalDate cutoffDate = LocalDate.now().minusDays(7);

            // when
            scheduler.cleanupOldData();

            // then
            verify(popularProductRepository, times(1)).deleteByBaseDateBefore(dateCaptor.capture());
            assertEquals(cutoffDate, dateCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("설정값 테스트")
    class ConfigurationTest {

        @Test
        @DisplayName("성공: topCount 설정에 따라 상위 N개만 조회한다")
        void topCount_설정_적용() {
            // given
            ReflectionTestUtils.setField(scheduler, "topCount", 3);
            LocalDate today = LocalDate.now();

            when(popularProductCustomRepository.aggregateTopSellingProducts(
                any(), any(), anyInt(), any()
            )).thenReturn(List.of());

            // when
            scheduler.aggregatePopularProducts();

            // then
            verify(popularProductCustomRepository, times(1))
                .aggregateTopSellingProducts(any(), any(), eq(3), any());
        }

        @Test
        @DisplayName("성공: periodDays 설정에 따라 집계 기간이 변경된다")
        void periodDays_설정_적용() {
            // given
            ReflectionTestUtils.setField(scheduler, "periodDays", 7);
            LocalDate today = LocalDate.now();
            LocalDate expectedStartDate = today.minusDays(7);

            when(popularProductCustomRepository.aggregateTopSellingProducts(
                any(), any(), anyInt(), any()
            )).thenReturn(List.of());

            // when
            scheduler.aggregatePopularProducts();

            // then
            verify(popularProductCustomRepository, times(1))
                .aggregateTopSellingProducts(eq(expectedStartDate), any(), anyInt(), any());
        }
    }
}
