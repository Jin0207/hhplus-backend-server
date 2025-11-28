package kr.hhplus.be.server.infrastructure.product.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQuery;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        throw new UnsupportedOperationException("인프라 계층 구현 시 실제 조회 로직을 작성하세요.");
    }

    @Override
    public Optional<Product> findById(Long id) {
        throw new UnsupportedOperationException("인프라 계층 구현 시 실제 조회 로직을 작성하세요.");
    }

    @Override
    public Page<ProductEntity> findBySearch(ProductSearch search, Pageable pageable) {
        throw new UnsupportedOperationException("인프라 계층 구현 시 실제 조회 로직을 작성하세요.");
    }

    
    @Override
    public List<Product> findPopularProducts() {
        throw new UnsupportedOperationException("인프라 계층 구현 시 실제 조회 로직을 작성하세요.");
    }
}