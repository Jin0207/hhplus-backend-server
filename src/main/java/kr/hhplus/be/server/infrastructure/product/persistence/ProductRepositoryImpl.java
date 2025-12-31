package kr.hhplus.be.server.infrastructure.product.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    
    @Override
    public Page<Product> findBySearch(ProductSearch search, Pageable pageable) {
        return productJpaRepository.findBySearch(search, pageable).map(ProductEntity::toDomain);
    }
    
    @Override
    public List<Product> findPopularProducts(){
        return productJpaRepository.findPopularProducts()
                .stream()
                .map(ProductEntity::toDomain)
                .toList();
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = ProductEntity.from(product);
        return productJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id).map(ProductEntity::toDomain);
    }

    @Override
    public Optional<Product> findByIdWithLock(Long id) {
        return productJpaRepository.findByIdWithLock(id).map(ProductEntity::toDomain);
    }

}