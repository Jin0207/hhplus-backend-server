package kr.hhplus.be.server.infrastructure.product.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockJpaRepository extends JpaRepository<StockEntity, Long>{
}
