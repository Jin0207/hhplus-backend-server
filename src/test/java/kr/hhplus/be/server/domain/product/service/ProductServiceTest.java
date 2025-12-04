package kr.hhplus.be.server.domain.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.infrastructure.product.persistence.ProductEntity;
import kr.hhplus.be.server.presentation.product.dto.response.PopularProductResponse;
import kr.hhplus.be.server.presentation.product.dto.response.ProductResponse;
import kr.hhplus.be.server.support.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 상품조회 TDD")
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductSearchCommand searchCommand;
    private List<Product> mockProductList;
    private Page<ProductEntity> mockProductEntityPage;
    
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
                1900000, 
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
                250000, 
                10, 
                ProductCategory.PANTS, 
                ProductStatus.ON_SALE, 
                20, 
                LocalDateTime.now(), 
                null
            )
        );

        // Entity 페이지 (Repository가 반환)
        List<ProductEntity> entityList = mockProductList.stream()
            .map(ProductEntity::from)
            .toList();
        
        Pageable pageable = PageRequest.of(0, 10);
        mockProductEntityPage = new PageImpl<>(entityList, pageable, entityList.size());
    }

    @Test
    @DisplayName("상품 목록 조회 - 데이터가 존재하는 경우 Page 객체 반환")
    public void 상품_목록_조회() {
        // given
        when(productRepository.findBySearch(
            any(ProductSearch.class), 
            any(Pageable.class)
        )).thenReturn(mockProductEntityPage);

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
        Page<ProductEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
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
        
        List<ProductEntity> filteredEntities = List.of(
            ProductEntity.from(mockProductList.get(0))
        );
        Page<ProductEntity> filteredPage = new PageImpl<>(
            filteredEntities, 
            PageRequest.of(0, 10), 
            1
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

    @Test
    @DisplayName("최근 3일 기준 누적판매량 상위 5개 상품 조회")
    public void 최근_3일_기준_누적판매량_상위_5개_상품_조회() {
// given
        when(productRepository.findPopularProducts())
            .thenReturn(mockProductList);

        // when
        List<PopularProductResponse> result = productService.findPopularProducts();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("상의1", result.get(0).productName());
        assertEquals("바지1", result.get(1).productName());
        assertEquals(10, result.get(0).salesQuantity());
        assertEquals(20, result.get(1).salesQuantity());
        
        verify(productRepository, times(1)).findPopularProducts();
    }

    @Test
    @DisplayName("인기 상품 조회 - 데이터가 없는 경우 빈 리스트 반환")
    void 인기상품_없음() {
        // given
        when(productRepository.findPopularProducts())
            .thenReturn(List.of());

        // when
        List<PopularProductResponse> result = productService.findPopularProducts();

        // then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("재고 차감 - 성공")
    void 재고_차감_성공() {
        // given
        Long productId = 1L;
        Integer quantity = 3;
        Product mockProduct = mockProductList.get(0);
        Product decreasedProduct = mockProduct.decreaseStock(quantity);
        
        when(productRepository.findById(productId))
            .thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class)))
            .thenReturn(decreasedProduct);

        // when
        Product result = productService.decreaseStock(productId, quantity);

        // then
        assertNotNull(result);
        assertEquals(7, result.stock()); // 10 - 3 = 7
        
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족으로 실패")
    void 재고_차감_실패_재고_부족() {
        // given
        Long productId = 1L;
        Integer quantity = 20; // 현재 재고(10)보다 많음
        Product mockProduct = mockProductList.get(0);
        
        when(productRepository.findById(productId))
            .thenReturn(Optional.of(mockProduct));

        // when & then
        assertThrows(BusinessException.class, () -> 
            productService.decreaseStock(productId, quantity)
        );
        
        verify(productRepository, times(1)).findById(productId);
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
