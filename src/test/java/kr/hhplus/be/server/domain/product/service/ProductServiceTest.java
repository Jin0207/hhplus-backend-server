package kr.hhplus.be.server.domain.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
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

    private ProductSearchCommand request;
    private List<Product> mockProducts;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        request = new ProductSearchCommand(
            null,  // name
            null,  // category
            null,  // status
            null,  // minPrice
            null,  // maxPrice
            0,     // page
            10     // size
        );

        mockProducts = List.of(
                new Product(1L, "상의1", 1900000, 10, ProductCategory.TOP, ProductStatus.ON_SALE, 10, null, null),
                new Product(2L, "바지1", 250000, 10, ProductCategory.PANTS, ProductStatus.ON_SALE, 20, null, null)
        );
    }

    @Test
    @DisplayName("상품 목록 조회 - 데이터가 존재하는 경우 Page 객체 반환")
    public void 상품_목록_조회() {
        // given
        when(productRepository.findProductsBySearch(any(ProductSearch.class), any(Pageable.class)))
            .thenReturn(mockProducts);
        when(productRepository.countProductsBySearch(any(ProductSearch.class)))
            .thenReturn((long) mockProducts.size());
        
        // when
        PageResponse<ProductResponse> result = productService.search(request);

        // then
        assertNotNull(result);
        assertEquals(2, result.contents().size());
        assertEquals("상의1", result.contents().get(0).productName());
        assertEquals("바지1", result.contents().get(1).productName());
        assertEquals(0, result.currentPage());
        assertEquals(2, result.totalElements());
        
        verify(productRepository, times(1))
            .findProductsBySearch(any(ProductSearch.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 데이터가 없는 경우 빈 Page 반환")
    void 빈_상품_목록_조회() {
        // given
        when(productRepository.findProductsBySearch(any(ProductSearch.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(productRepository.countProductsBySearch(any(ProductSearch.class)))
            .thenReturn(0L);

        // when
        PageResponse<ProductResponse> result = productService.search(request);

        // then
        assertNotNull(result);
        assertEquals(0, result.contents().size());
        assertEquals(0, result.totalElements());
    }

    @Test
    @DisplayName("최근 3일 기준 누적판매량 상위 5개 상품 조회")
    public void 최근_3일_기준_누적판매량_상위_5개_상품_조회() {
        // given
        when(productRepository.findPopularProducts()).thenReturn(mockProducts);

        List<PopularProductResponse> result = productService.findPopularProducts();
        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("상의1", result.get(0).productName());
        assertEquals("바지1", result.get(1).productName());
    }

    @Test
    @DisplayName("가격 범위가 잘못되면 예외가 발생한다")
    void 가격범위_검증() {
        assertThrows(BusinessException.class, () ->
            new ProductSearchCommand(null, null, null, 1000, 500, 0, 10)
        );
    }
}
