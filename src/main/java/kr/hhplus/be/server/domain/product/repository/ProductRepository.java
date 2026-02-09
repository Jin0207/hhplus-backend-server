package kr.hhplus.be.server.domain.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.infrastructure.product.persistence.ProductEntity;

public interface ProductRepository {

    /**
     * 상품 저장
     */
    Product save(Product product);

    /**
     * 상품 단건 조회
     */
    Optional<Product> findById(Long productId);

    /**
     * 상품 단건 조회 (비관적 락 - Pessimistic Lock)
     * 재고 차감 시 동시성 제어를 위해 사용
     */
    Optional<Product> findByIdWithLock(Long productId);

    /**
     * 상품 검색
     */
    Page<Product> findBySearch(ProductSearch search, Pageable pageable);

    /**
     * 인기 상품 조회
     */
    List<Product> findPopularProducts();

    /**
     * 여러 상품 ID로 조회
     */
    List<Product> findAllById(Iterable<Long> ids);
}
