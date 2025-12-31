package kr.hhplus.be.server.infrastructure.product.persistence;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepository {
    private final StockJpaRepository stockJpaRepository;

    @Override
    public Stock save(Stock stock) {
        StockEntity entity = StockEntity.from(stock);
        return stockJpaRepository.save(entity).toDomain();
    }
}