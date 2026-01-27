package kr.hhplus.be.server.infrastructure.product.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.repository.PopularProductRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PopularProductRepositoryImpl implements PopularProductRepository {

    private final PopularProductJpaRepository popularProductJpaRepository;

    @Override
    public PopularProduct save(PopularProduct popularProduct) {
        PopularProductEntity entity = PopularProductEntity.from(popularProduct);
        return popularProductJpaRepository.save(entity).toDomain();
    }

    @Override
    public List<PopularProduct> saveAll(List<PopularProduct> popularProducts) {
        List<PopularProductEntity> entities = popularProducts.stream()
            .map(PopularProductEntity::from)
            .toList();
        return popularProductJpaRepository.saveAll(entities).stream()
            .map(PopularProductEntity::toDomain)
            .toList();
    }

    @Override
    public List<PopularProduct> findByBaseDate(LocalDate baseDate) {
        return popularProductJpaRepository.findByBaseDateOrderByRankAsc(baseDate).stream()
            .map(PopularProductEntity::toDomain)
            .toList();
    }

    @Override
    public List<PopularProduct> findLatest() {
        return popularProductJpaRepository.findLatest().stream()
            .map(PopularProductEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteByBaseDate(LocalDate baseDate) {
        popularProductJpaRepository.deleteByBaseDate(baseDate);
    }

    @Override
    public void deleteByBaseDateBefore(LocalDate baseDate) {
        popularProductJpaRepository.deleteByBaseDateBefore(baseDate);
    }
}
