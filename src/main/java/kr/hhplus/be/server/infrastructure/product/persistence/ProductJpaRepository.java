package kr.hhplus.be.server.infrastructure.product.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, ProductCustomRepository{

    /**
     * 비관적 락(Pessimistic Lock)을 사용한 상품 조회
     * 재고 차감 시 동시성 제어를 위해 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findByIdWithLock(@Param("id") Long id);
}
