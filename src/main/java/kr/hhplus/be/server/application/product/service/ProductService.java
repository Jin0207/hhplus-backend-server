package kr.hhplus.be.server.application.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;
import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.repository.PopularProductRepository;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.presentation.product.dto.response.PopularProductResponse;
import kr.hhplus.be.server.presentation.product.dto.response.ProductResponse;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final PopularProductRepository popularProductRepository;
    
    /**
     * 상품 단건 검색
     */
    public Product getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return product;
    }
    
    /**
     * 재고 차감 (주문 시)
     * 비관적 락(Pessimistic Lock)을 사용하여 동시성 제어
     * OrderFacade의 트랜잭션에서 호출되므로 @Transactional 불필요
     */
    public Product decreaseStock(Long productId, Integer quantity) {
        // 비관적 락을 사용하여 조회 (동시성 제어)
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 주문 가능 여부 검증
        product.validateForOrder(quantity);

        // 재고 차감
        Product updatedProduct = product.decreaseStock(quantity);

        return productRepository.save(updatedProduct);
    }

    /**
     * 재고 증가
     * 주문 취소 시에는 OrderCancellationService의 트랜잭션에서 호출
     */
    @Transactional
    public Product increaseStock(Long productId, Integer quantity) {
        Product product = getProduct(productId);

        // 재고 증가
        Product updatedProduct = product.increaseStock(quantity);

        return productRepository.save(updatedProduct);
    }

    /**
     * 판매량 증가 (주문 완료 시)
     * OrderFacade의 트랜잭션에서 호출되므로 @Transactional 불필요
     */
    public Product increaseSalesQuantity(Long productId, Integer quantity) {
        Product product = getProduct(productId);
        Product updatedProduct = product.increaseSalesQuantity(quantity);

        return productRepository.save(updatedProduct);
    }

    /**
     * 판매량 감소 (주문 취소 시)
     */
    @Transactional
    public Product decreaseSalesQuantity(Long productId, Integer quantity) {
        Product product = getProduct(productId);

        // 판매량이 0 미만으로 내려가지 않도록 보호
        Integer newSalesQuantity = Math.max(0, product.salesQuantity() - quantity);
        Integer actualDecrease = product.salesQuantity() - newSalesQuantity;

        if (actualDecrease > 0) {
            Product updatedProduct = new Product(
                product.id(),
                product.productName(),
                product.price(),
                product.stock(),
                product.category(),
                product.status(),
                newSalesQuantity,
                product.crtDttm(),
                java.time.LocalDateTime.now()
            );
            return productRepository.save(updatedProduct);
        }

        return product;
    }
    
    /**
     * 상품 검색 (페이징)
     */
    public PageResponse<ProductResponse> search(ProductSearchCommand request) {
        Pageable pageable = request.toPageable();
        
        Page<Product> page = productRepository.findBySearch(
            request.toDomain(), 
            pageable
        );
        
        List<ProductResponse> responses = page.getContent().stream()
            .map(ProductResponse::from)
            .toList();
        
        Page<ProductResponse> responsePage = new PageImpl<>(
            responses, 
            pageable, 
            page.getTotalElements()
        );
        
        return PageResponse.of(responsePage);
    }

    /**
     * 인기 상품 조회 (최근 3일 기준 상위 5개)
     * - 우선: 캐시 테이블(popular_products)에서 조회
     * - Fallback: 캐시가 없으면 실시간 집계
     */
    public List<PopularProductResponse> findPopularProducts() {
        // 1. 캐시 테이블에서 최신 데이터 조회
        List<PopularProduct> cachedProducts = popularProductRepository.findLatest();

        if (!cachedProducts.isEmpty()) {
            log.debug("[인기상품] 캐시 테이블 조회: {} 건", cachedProducts.size());
            return cachedProducts.stream()
                .map(PopularProductResponse::from)
                .toList();
        }

        // 2. 캐시가 없으면 실시간 조회 (fallback)
        log.debug("[인기상품] 캐시 없음, 실시간 조회");
        List<Product> popularProducts = productRepository.findPopularProducts();

        return popularProducts.stream()
            .map(PopularProductResponse::from)
            .toList();
    }
    
}
