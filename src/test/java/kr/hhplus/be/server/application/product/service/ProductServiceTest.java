package kr.hhplus.be.server.application.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;
import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.PopularProductRepository;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.presentation.product.dto.response.PopularProductResponse;
import kr.hhplus.be.server.presentation.product.dto.response.ProductResponse;
import kr.hhplus.be.server.support.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 상품조회 TDD")
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private PopularProductRepository popularProductRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ProductService productService;

    private ProductSearchCommand searchCommand;
    private List<Product> mockProductList;
    private Page<Product> mockProductPage;
    
    @BeforeEach
    void setUp() {
        // 검색 조건 (전체 조회)
        searchCommand = new ProductSearchCommand(
            null,  // name
            null,  // category
            null,  // status
            null,  // minPrice
            null,  // maxPrice
            0,     // page
            10     // size
        );

        // Domain 객체 리스트
        mockProductList = List.of(
            new Product(
                1L, 
                "상의1", 
                1900000L, 
                10, 
                ProductCategory.TOP, 
                ProductStatus.ON_SALE, 
                10, 
                LocalDateTime.now(), 
                null
            ),
            new Product(
                2L, 
                "바지1", 
                250000L, 
                10, 
                ProductCategory.PANTS, 
                ProductStatus.ON_SALE, 
                20, 
                LocalDateTime.now(), 
                null
            )
        );
        
        Pageable pageable = PageRequest.of(0, 10);
        mockProductPage = new PageImpl<>(
            mockProductList,
            pageable,
            mockProductList.size()
        );
    }

    @Test
    @DisplayName("상품 목록 조회 - 데이터가 존재하는 경우 Page 객체 반환")
    public void 상품_목록_조회() {
        // given
        when(productRepository.findBySearch(
            any(ProductSearch.class), 
            any(Pageable.class)
        )).thenReturn(mockProductPage);

        // when
        PageResponse<ProductResponse> result = productService.search(searchCommand);

        // then
        assertNotNull(result);
        assertEquals(2, result.contents().size());
        assertEquals("상의1", result.contents().get(0).productName());
        assertEquals("바지1", result.contents().get(1).productName());
        assertEquals(0, result.currentPage());
        assertEquals(2, result.totalElements());
        
        verify(productRepository, times(1))
            .findBySearch(any(ProductSearch.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 데이터가 없는 경우 빈 Page 반환")
    void 빈_상품_목록_조회() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(productRepository.findBySearch(
            any(ProductSearch.class), 
            any(Pageable.class)
        )).thenReturn(emptyPage);

        // when
        PageResponse<ProductResponse> result = productService.search(searchCommand);

        // then
        assertNotNull(result);
        assertEquals(0, result.contents().size());
        assertEquals(0, result.totalElements());
        
        verify(productRepository, times(1))
            .findBySearch(any(ProductSearch.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 검색 - 특정 조건으로 검색")
    void 조건별_상품_검색() {
        // given
        ProductSearchCommand specificSearch = new ProductSearchCommand(
            "상의",                    // name
            "TOP",       // category
            "ON_SALE",     // status
            1000000,                   // minPrice
            2000000,                   // maxPrice
            0,
            10
        );
        
        List<Product> filteredProducts = List.of(
            mockProductList.get(0) // "상의1"
        );
        
        Page<Product> filteredPage = new PageImpl<>(
            filteredProducts,
            PageRequest.of(0, 10),
            filteredProducts.size()
        );

        when(productRepository.findBySearch(
            any(ProductSearch.class), 
            any(Pageable.class)
        )).thenReturn(filteredPage);

        // when
        PageResponse<ProductResponse> result = productService.search(specificSearch);

        // then
        assertNotNull(result);
        assertEquals(1, result.contents().size());
        assertEquals("상의1", result.contents().get(0).productName());
        assertEquals(1900000, result.contents().get(0).price());
    }

    @Test
    @DisplayName("가격 범위가 잘못되면 예외가 발생한다")
    void 가격범위_검증() {
        assertThrows(BusinessException.class, () ->
            new ProductSearchCommand(null, null, null, 1000, 500, 0, 10)
        );
    }

    // ==================== 인기 상품 조회 테스트 (Redis Cache-Aside + DB캐시 + Fallback) ====================

    @Nested
    @DisplayName("인기 상품 조회 (findPopularProducts)")
    class FindPopularProducts {

        private List<PopularProduct> mockCachedProducts;

        @BeforeEach
        void setUpPopularProducts() {
            LocalDate baseDate = LocalDate.now();
            mockCachedProducts = List.of(
                PopularProduct.fromAggregation(1, 1L, "인기상품1", 50000L, ProductCategory.TOP, 150, baseDate),
                PopularProduct.fromAggregation(2, 2L, "인기상품2", 30000L, ProductCategory.PANTS, 120, baseDate),
                PopularProduct.fromAggregation(3, 3L, "인기상품3", 80000L, ProductCategory.OUTER, 100, baseDate)
            );
            // Redis opsForValue mock 기본 설정
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        @DisplayName("성공: Redis 캐시에 데이터가 있으면 Redis에서 즉시 반환한다")
        void Redis_캐시_Hit() {
            // given
            List<PopularProductResponse> cachedResponses = mockCachedProducts.stream()
                .map(PopularProductResponse::from)
                .toList();
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenReturn(cachedResponses);
            when(objectMapper.convertValue(any(), any(TypeReference.class)))
                .thenReturn(cachedResponses);

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("인기상품1", result.get(0).productName());

            // Redis Hit이므로 DB 접근 없음
            verify(popularProductRepository, never()).findLatest();
            verify(productRepository, never()).findPopularProducts();
        }

        @Test
        @DisplayName("성공: Redis 캐시 Miss 시 DB 캐시 테이블에서 조회하고 Redis에 저장한다")
        void Redis_Miss_DB캐시_Hit() {
            // given
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenReturn(null); // Redis Miss
            when(popularProductRepository.findLatest())
                .thenReturn(mockCachedProducts);

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("인기상품1", result.get(0).productName());
            assertEquals(150, result.get(0).salesQuantity());

            // DB 캐시 테이블 조회 + Redis에 저장
            verify(popularProductRepository, times(1)).findLatest();
            verify(valueOperations, times(1)).set(
                eq(ProductService.POPULAR_PRODUCTS_CACHE_KEY),
                any(),
                eq(ProductService.POPULAR_PRODUCTS_CACHE_TTL_HOURS),
                eq(TimeUnit.HOURS)
            );
            verify(productRepository, never()).findPopularProducts();
        }

        @Test
        @DisplayName("성공: Redis Miss + DB 캐시 없으면 실시간 조회로 Fallback하고 Redis에 저장한다")
        void Redis_Miss_DB캐시_Miss_실시간_Fallback() {
            // given
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenReturn(null);
            when(popularProductRepository.findLatest())
                .thenReturn(List.of());
            when(productRepository.findPopularProducts())
                .thenReturn(mockProductList);

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("상의1", result.get(0).productName());

            verify(popularProductRepository, times(1)).findLatest();
            verify(productRepository, times(1)).findPopularProducts();
            // 실시간 조회 결과도 Redis에 저장
            verify(valueOperations, times(1)).set(
                eq(ProductService.POPULAR_PRODUCTS_CACHE_KEY),
                any(),
                eq(ProductService.POPULAR_PRODUCTS_CACHE_TTL_HOURS),
                eq(TimeUnit.HOURS)
            );
        }

        @Test
        @DisplayName("성공: 모든 캐시와 실시간 모두 데이터 없으면 빈 리스트 반환 (Redis 저장 안함)")
        void 인기상품_없음_빈_리스트() {
            // given
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenReturn(null);
            when(popularProductRepository.findLatest())
                .thenReturn(List.of());
            when(productRepository.findPopularProducts())
                .thenReturn(List.of());

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertEquals(0, result.size());

            verify(popularProductRepository, times(1)).findLatest();
            verify(productRepository, times(1)).findPopularProducts();
            // 빈 결과는 Redis에 저장하지 않음
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("성공: Redis 장애 시 DB 캐시 테이블로 Fallback한다")
        void Redis_장애시_DB_Fallback() {
            // given
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenThrow(new RuntimeException("Redis connection refused"));
            when(popularProductRepository.findLatest())
                .thenReturn(mockCachedProducts);

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("인기상품1", result.get(0).productName());

            // Redis 장애 시에도 DB 캐시로 정상 응답
            verify(popularProductRepository, times(1)).findLatest();
        }

        @Test
        @DisplayName("성공: DB 캐시 테이블 데이터는 재고/상태 정보가 null이다")
        void 캐시_데이터_스냅샷_검증() {
            // given
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenReturn(null);
            when(popularProductRepository.findLatest())
                .thenReturn(mockCachedProducts);

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertNull(result.get(0).stock());
            assertNull(result.get(0).status());
            assertEquals(150, result.get(0).salesQuantity());
        }

        @Test
        @DisplayName("성공: 실시간 조회 데이터는 재고/상태 정보가 존재한다")
        void 실시간_데이터_전체_정보_검증() {
            // given
            when(valueOperations.get(ProductService.POPULAR_PRODUCTS_CACHE_KEY))
                .thenReturn(null);
            when(popularProductRepository.findLatest())
                .thenReturn(List.of());
            when(productRepository.findPopularProducts())
                .thenReturn(mockProductList);

            // when
            List<PopularProductResponse> result = productService.findPopularProducts();

            // then
            assertNotNull(result);
            assertEquals(10, result.get(0).stock());
            assertEquals(ProductStatus.ON_SALE, result.get(0).status());
        }
    }

    @Test
    @DisplayName("재고 차감 - 성공")
    void 재고_차감_성공() {
        // given
        Long productId = 1L;
        Integer quantity = 3;
        Product mockProduct = mockProductList.get(0);
        Product decreasedProduct = mockProduct.decreaseStock(quantity);
        
        when(productRepository.findByIdWithLock(productId))
            .thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class)))
            .thenReturn(decreasedProduct);

        // when
        Product result = productService.decreaseStock(productId, quantity);

        // then
        assertNotNull(result);
        assertEquals(7, result.stock()); // 10 - 3 = 7

        verify(productRepository, times(1)).findByIdWithLock(productId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족으로 실패")
    void 재고_차감_실패_재고_부족() {
        // given
        Long productId = 1L;
        Integer quantity = 20; // 현재 재고(10)보다 많음
        Product mockProduct = mockProductList.get(0);

        when(productRepository.findByIdWithLock(productId))
            .thenReturn(Optional.of(mockProduct));

        // when & then
        assertThrows(BusinessException.class, () ->
            productService.decreaseStock(productId, quantity)
        );

        verify(productRepository, times(1)).findByIdWithLock(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    // ==================== 판매량 증가 테스트 ====================

    @Test
    @DisplayName("판매량 증가 - 성공")
    void 판매량_증가_성공() {
        // given
        Long productId = 1L;
        Integer quantity = 5;
        Product mockProduct = mockProductList.get(0);
        Product increasedProduct = mockProduct.increaseSalesQuantity(quantity);
        
        when(productRepository.findById(productId))
            .thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class)))
            .thenReturn(increasedProduct);

        // when
        Product result = productService.increaseSalesQuantity(productId, quantity);

        // then
        assertNotNull(result);
        assertEquals(15, result.salesQuantity()); // 10 + 5 = 15
        
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

}
