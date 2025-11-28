package kr.hhplus.be.server.infrastructure.product.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProductCustomReopsitoryImpl implements ProductCustomRepository {

    private final ProductJpaRepository productJpaRepository;
    
    @Override
    public Page<ProductEntity> findBySearch(ProductSearch search, Pageable pageable) {
        throw new UnsupportedOperationException("인프라 계층 구현 시 실제 조회 로직을 작성하세요.");

    }
    
    @Override
    public List<ProductEntity> findPopularProducts() {
        throw new UnsupportedOperationException("인프라 계층 구현 시 실제 조회 로직을 작성하세요.");
    }
}
