package kr.hhplus.be.server.domain.product;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;

@DisplayName("PopularProduct 도메인 테스트")
class PopularProductTest {

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("성공: Product로부터 PopularProduct 생성 시 기간이 올바르게 계산된다")
        void Product로부터_생성_성공() {
            // given
            Product product = new Product(
                1L, "인기상품", 50000L, 100,
                ProductCategory.TOP, ProductStatus.ON_SALE,
                500, LocalDateTime.now(), null
            );
            LocalDate baseDate = LocalDate.of(2024, 1, 15);
            Integer rank = 1;
            Integer totalSalesQuantity = 150;

            // when
            PopularProduct popularProduct = PopularProduct.create(
                rank, product, totalSalesQuantity, baseDate
            );

            // then
            assertThat(popularProduct.id()).isNull();
            assertThat(popularProduct.rank()).isEqualTo(1);
            assertThat(popularProduct.productId()).isEqualTo(1L);
            assertThat(popularProduct.productName()).isEqualTo("인기상품");
            assertThat(popularProduct.price()).isEqualTo(50000L);
            assertThat(popularProduct.category()).isEqualTo(ProductCategory.TOP);
            assertThat(popularProduct.totalSalesQuantity()).isEqualTo(150);
            assertThat(popularProduct.baseDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(popularProduct.periodStartDate()).isEqualTo(LocalDate.of(2024, 1, 12)); // D-3
            assertThat(popularProduct.periodEndDate()).isEqualTo(LocalDate.of(2024, 1, 14));   // D-1
        }

        @Test
        @DisplayName("성공: 집계 기간이 기준일 기준 D-3 ~ D-1로 계산된다")
        void 집계_기간_계산_검증() {
            // given
            Product product = new Product(
                2L, "테스트상품", 30000L, 50,
                ProductCategory.PANTS, ProductStatus.ON_SALE,
                200, LocalDateTime.now(), null
            );
            LocalDate baseDate = LocalDate.of(2024, 3, 1);

            // when
            PopularProduct popularProduct = PopularProduct.create(
                2, product, 80, baseDate
            );

            // then
            assertThat(popularProduct.periodStartDate()).isEqualTo(LocalDate.of(2024, 2, 27)); // D-3
            assertThat(popularProduct.periodEndDate()).isEqualTo(LocalDate.of(2024, 2, 29));   // D-1 (윤년)
        }
    }

    @Nested
    @DisplayName("fromAggregation 메서드")
    class FromAggregation {

        @Test
        @DisplayName("성공: 집계 결과로부터 PopularProduct 생성")
        void 집계_결과로부터_생성_성공() {
            // given
            LocalDate baseDate = LocalDate.of(2024, 6, 10);

            // when
            PopularProduct popularProduct = PopularProduct.fromAggregation(
                1,
                100L,
                "베스트상품",
                99000L,
                ProductCategory.OUTER,
                350,
                baseDate
            );

            // then
            assertThat(popularProduct.rank()).isEqualTo(1);
            assertThat(popularProduct.productId()).isEqualTo(100L);
            assertThat(popularProduct.productName()).isEqualTo("베스트상품");
            assertThat(popularProduct.price()).isEqualTo(99000L);
            assertThat(popularProduct.category()).isEqualTo(ProductCategory.OUTER);
            assertThat(popularProduct.totalSalesQuantity()).isEqualTo(350);
            assertThat(popularProduct.baseDate()).isEqualTo(baseDate);
            assertThat(popularProduct.periodStartDate()).isEqualTo(LocalDate.of(2024, 6, 7));
            assertThat(popularProduct.periodEndDate()).isEqualTo(LocalDate.of(2024, 6, 9));
        }

        @Test
        @DisplayName("성공: 카테고리가 null인 경우에도 생성된다")
        void 카테고리_null_허용() {
            // given
            LocalDate baseDate = LocalDate.now();

            // when
            PopularProduct popularProduct = PopularProduct.fromAggregation(
                3, 50L, "카테고리없는상품", 15000L, null, 25, baseDate
            );

            // then
            assertThat(popularProduct.category()).isNull();
            assertThat(popularProduct.productName()).isEqualTo("카테고리없는상품");
        }
    }

    @Nested
    @DisplayName("순위 관련 테스트")
    class RankTest {

        @Test
        @DisplayName("성공: 순위 1~5까지 생성 가능")
        void 순위_범위_테스트() {
            // given
            LocalDate baseDate = LocalDate.now();

            // when & then
            for (int rank = 1; rank <= 5; rank++) {
                PopularProduct product = PopularProduct.fromAggregation(
                    rank, (long) rank, "상품" + rank, 10000L * rank,
                    ProductCategory.TOP, rank * 10, baseDate
                );
                assertThat(product.rank()).isEqualTo(rank);
            }
        }
    }

    @Nested
    @DisplayName("스냅샷 데이터 테스트")
    class SnapshotTest {

        @Test
        @DisplayName("성공: 상품 정보가 스냅샷으로 저장된다 (원본 Product와 독립적)")
        void 스냅샷_독립성_검증() {
            // given
            Product originalProduct = new Product(
                1L, "원본상품", 10000L, 50,
                ProductCategory.TOP, ProductStatus.ON_SALE,
                100, LocalDateTime.now(), null
            );
            LocalDate baseDate = LocalDate.now();

            // when
            PopularProduct popularProduct = PopularProduct.create(
                1, originalProduct, 30, baseDate
            );

            // then - PopularProduct는 Product와 별개로 스냅샷 데이터를 가짐
            assertThat(popularProduct.productName()).isEqualTo("원본상품");
            assertThat(popularProduct.price()).isEqualTo(10000L);
            assertThat(popularProduct.totalSalesQuantity()).isEqualTo(30); // 집계 기간 판매량

            // Product의 salesQuantity와 PopularProduct의 totalSalesQuantity는 다른 값
            assertThat(popularProduct.totalSalesQuantity()).isNotEqualTo(originalProduct.salesQuantity());
        }
    }
}
