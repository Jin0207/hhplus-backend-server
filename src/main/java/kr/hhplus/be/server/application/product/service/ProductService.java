package kr.hhplus.be.server.application.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.product.dto.request.ProductSearchCommand;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.infrastructure.product.persistence.ProductEntity;
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
     */
    @Transactional
    public Product decreaseStock(Long productId, Integer quantity) {
        Product product = getProduct(productId);
        
        // 주문 가능 여부 검증
        product.validateForOrder(quantity);
        
        // 재고 차감
        Product updatedProduct = product.decreaseStock(quantity);
        
        return productRepository.save(updatedProduct);
    }

    /**
     * 판매량 증가 (주문 완료 시)
     */
    @Transactional
    public Product increaseSalesQuantity(Long productId, Integer quantity) {
        Product product = getProduct(productId);
        Product updatedProduct = product.increaseSalesQuantity(quantity);
        
        return productRepository.save(updatedProduct);
    }
    
    /**
     * 상품 검색 (페이징)
     */
    public PageResponse<ProductResponse> search(ProductSearchCommand request) {
        Pageable pageable = request.toPageable();
        
        Page<ProductEntity> page = productRepository.findBySearch(
            request.toDomain(), 
            pageable
        );
        
        List<ProductResponse> responses = page.getContent().stream()
            .map(ProductEntity::toDomain)
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
     */
    public List<PopularProductResponse> findPopularProducts() {
        List<Product> popularProducts = productRepository.findPopularProducts();
        
        return popularProducts.stream()
            .map(PopularProductResponse::from)
            .toList();
    }
    
}
