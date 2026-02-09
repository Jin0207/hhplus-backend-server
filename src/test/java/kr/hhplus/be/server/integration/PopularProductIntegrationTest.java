package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.redis.core.RedisTemplate;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.PopularProductRepository;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.infrastructure.product.PopularProductScheduler;
import kr.hhplus.be.server.presentation.product.dto.response.PopularProductResponse;

/**
 * 인기 상품 배치 집계 통합 테스트
 */
class PopularProductIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PopularProductScheduler popularProductScheduler;

    @Autowired
    private PopularProductRepository popularProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User testUser;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화 (테스트 격리)
        redisTemplate.delete(ProductService.POPULAR_PRODUCTS_CACHE_KEY);

        // 테스트 사용자 생성
        long timestamp = System.currentTimeMillis();
        testUser = userRepository.save(User.create("popular_test_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 1000000L, "테스트 충전"); // 최대 100만 포인트

        // 테스트 상품 생성 (높은 재고, 낮은 가격으로 대량 주문 테스트 가능)
        testProducts = List.of(
            productRepository.save(new Product(null, "인기상품A", 100L, 1000, ProductCategory.TOP, ProductStatus.ON_SALE, 0, LocalDateTime.now(), null)),
            productRepository.save(new Product(null, "인기상품B", 200L, 1000, ProductCategory.PANTS, ProductStatus.ON_SALE, 0, LocalDateTime.now(), null)),
            productRepository.save(new Product(null, "인기상품C", 300L, 1000, ProductCategory.OUTER, ProductStatus.ON_SALE, 0, LocalDateTime.now(), null))
        );
    }

    @Nested
    @DisplayName("배치 집계 테스트")
    class BatchAggregationTest {

        @Test
        @DisplayName("성공: 주문 데이터 기반으로 인기 상품을 집계한다")
        void 주문_기반_인기상품_집계() {
            // Given: 각 상품별로 다른 수량의 주문 생성 (다른 테스트보다 높은 수량 사용)
            // 상품A: 500개, 상품B: 300개, 상품C: 100개 주문
            createOrder(testProducts.get(0).id(), 500); // 인기상품A
            createOrder(testProducts.get(1).id(), 300); // 인기상품B
            createOrder(testProducts.get(2).id(), 100); // 인기상품C

            // When: 배치 집계 실행
            popularProductScheduler.aggregatePopularProducts();

            // Then: 집계 결과 확인
            LocalDate today = LocalDate.now();
            List<PopularProduct> results = popularProductRepository.findByBaseDate(today);

            assertThat(results).isNotEmpty();

            // 테스트 상품이 결과에 포함되어 있는지 확인 (productId로 검증)
            PopularProduct productA = results.stream()
                .filter(p -> p.productId().equals(testProducts.get(0).id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("인기상품A가 결과에 없음"));

            assertThat(productA.productName()).isEqualTo("인기상품A");
            assertThat(productA.totalSalesQuantity()).isGreaterThanOrEqualTo(500); // 누적 판매량 500 이상
            assertThat(productA.rank()).isGreaterThanOrEqualTo(1); // 집계 결과에 포함됨

            // 상품B도 확인 (결과에 포함되어 있으면 검증, 없으면 top-count 제한으로 인해 제외됨)
            results.stream()
                .filter(p -> p.productId().equals(testProducts.get(1).id()))
                .findFirst()
                .ifPresent(productB -> {
                    assertThat(productB.productName()).isEqualTo("인기상품B");
                    assertThat(productB.totalSalesQuantity()).isGreaterThanOrEqualTo(300);
                });
        }

        @Test
        @DisplayName("성공: 배치 재실행 시 기존 데이터를 덮어쓴다 (멱등성)")
        void 배치_멱등성_검증() {
            // Given: 초기 주문 및 배치 실행 (높은 수량 사용)
            createOrder(testProducts.get(0).id(), 300);
            popularProductScheduler.aggregatePopularProducts();

            LocalDate today = LocalDate.now();
            List<PopularProduct> firstResults = popularProductRepository.findByBaseDate(today);

            // 테스트 상품 확인
            PopularProduct firstProduct = firstResults.stream()
                .filter(p -> p.productId().equals(testProducts.get(0).id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("테스트 상품이 결과에 없음"));
            assertThat(firstProduct.totalSalesQuantity()).isEqualTo(300);

            // When: 추가 주문 후 배치 재실행
            createOrder(testProducts.get(0).id(), 200); // 추가 200개 주문
            popularProductScheduler.aggregatePopularProducts();

            // Then: 새로운 집계 결과로 덮어쓰여짐
            List<PopularProduct> secondResults = popularProductRepository.findByBaseDate(today);

            PopularProduct secondProduct = secondResults.stream()
                .filter(p -> p.productId().equals(testProducts.get(0).id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("테스트 상품이 결과에 없음"));
            assertThat(secondProduct.totalSalesQuantity()).isEqualTo(500); // 300 + 200 = 500
        }

        @Test
        @DisplayName("성공: 집계 기간 정보가 정확히 저장된다")
        void 집계_기간_정보_검증() {
            // Given: 높은 수량으로 주문 생성
            createOrder(testProducts.get(0).id(), 400);

            // When
            popularProductScheduler.aggregatePopularProducts();

            // Then
            LocalDate today = LocalDate.now();
            List<PopularProduct> results = popularProductRepository.findByBaseDate(today);

            assertThat(results).isNotEmpty();

            // 테스트 상품 찾기
            PopularProduct result = results.stream()
                .filter(p -> p.productId().equals(testProducts.get(0).id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("테스트 상품이 결과에 없음"));

            assertThat(result.baseDate()).isEqualTo(today);
            // PopularProduct.fromAggregation()에서 하드코딩된 값 검증
            assertThat(result.periodStartDate()).isEqualTo(today.minusDays(3)); // D-3
            assertThat(result.periodEndDate()).isEqualTo(today.minusDays(1));   // D-1
        }
    }

    @Nested
    @DisplayName("API 조회 테스트")
    class ApiQueryTest {

        @Test
        @DisplayName("성공: 캐시 테이블에서 인기 상품을 조회한다")
        void 캐시_테이블_조회() {
            // Given: 배치 실행하여 캐시 테이블에 데이터 저장 (높은 수량 사용)
            createOrder(testProducts.get(0).id(), 600);
            createOrder(testProducts.get(1).id(), 350);
            popularProductScheduler.aggregatePopularProducts();

            // When: API 조회
            List<PopularProductResponse> response = productService.findPopularProducts();

            // Then: 캐시 테이블 데이터 반환
            assertThat(response).isNotEmpty();

            // 테스트 상품이 결과에 포함되어 있는지 확인 (id로 검증)
            PopularProductResponse productA = response.stream()
                .filter(p -> p.id().equals(testProducts.get(0).id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("인기상품A가 결과에 없음"));

            assertThat(productA.productName()).isEqualTo("인기상품A");
            assertThat(productA.salesQuantity()).isEqualTo(600);
        }

        @Test
        @Transactional
        @DisplayName("성공: 캐시 테이블이 비어있으면 실시간 조회로 Fallback한다")
        void 실시간_조회_Fallback() {
            // Given: 캐시 비어있음 (배치 미실행)
            // Redis 캐시 + DB 캐시 테이블 삭제하여 Fallback 조건 충족
            redisTemplate.delete(ProductService.POPULAR_PRODUCTS_CACHE_KEY);
            popularProductRepository.deleteByBaseDate(LocalDate.now());

            // 주문만 생성 (배치 실행 안함) - 높은 수량 사용
            createOrder(testProducts.get(0).id(), 700);

            // When: API 조회
            List<PopularProductResponse> response = productService.findPopularProducts();

            // Then: 실시간 조회 결과 반환
            assertThat(response).isNotEmpty();
            // 실시간 조회는 stock, status 정보 포함
            assertThat(response.get(0).stock()).isNotNull();
            assertThat(response.get(0).status()).isNotNull();
        }
    }

    @Nested
    @DisplayName("데이터 정리 테스트")
    class CleanupTest {

        @Test
        @Transactional
        @DisplayName("성공: 오래된 데이터가 삭제된다")
        void 오래된_데이터_삭제() {
            // Given: 8일 전 데이터 직접 삽입
            LocalDate oldDate = LocalDate.now().minusDays(8);
            PopularProduct oldData = PopularProduct.fromAggregation(
                1, 999L, "오래된상품", 10000L, ProductCategory.TOP, 100, oldDate
            );
            popularProductRepository.save(oldData);

            // 오늘 데이터도 삽입
            createOrder(testProducts.get(0).id(), 1);
            popularProductScheduler.aggregatePopularProducts();

            // When: 정리 배치 실행
            popularProductScheduler.cleanupOldData();

            // Then: 8일 전 데이터 삭제됨
            List<PopularProduct> oldResults = popularProductRepository.findByBaseDate(oldDate);
            assertThat(oldResults).isEmpty();

            // 오늘 데이터는 유지됨
            List<PopularProduct> todayResults = popularProductRepository.findByBaseDate(LocalDate.now());
            assertThat(todayResults).isNotEmpty();
        }
    }

    /**
     * 주문 생성 헬퍼 메서드
     */
    private void createOrder(Long productId, int quantity) {
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(productId, quantity)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );
        orderFacade.completeOrder(testUser.id(), orderRequest);
    }
}
