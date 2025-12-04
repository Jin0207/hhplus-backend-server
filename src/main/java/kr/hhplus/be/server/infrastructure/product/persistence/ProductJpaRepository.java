package kr.hhplus.be.server.infrastructure.product.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, ProductCustomRepository{
}
