package kr.hhplus.be.server.infrastructure.product.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopularProductJpaRepository extends JpaRepository<PopularProductEntity, Long> {

    /**
     * 기준일로 인기 상품 조회 (순위 오름차순)
     */
    List<PopularProductEntity> findByBaseDateOrderByRankAsc(LocalDate baseDate);

    /**
     * 가장 최신 기준일의 인기 상품 조회
     */
    @Query("SELECT p FROM PopularProductEntity p WHERE p.baseDate = " +
           "(SELECT MAX(p2.baseDate) FROM PopularProductEntity p2) ORDER BY p.rank ASC")
    List<PopularProductEntity> findLatest();

    /**
     * 기준일 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM PopularProductEntity p WHERE p.baseDate = :baseDate")
    void deleteByBaseDate(@Param("baseDate") LocalDate baseDate);

    /**
     * 특정 일자 이전 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM PopularProductEntity p WHERE p.baseDate < :baseDate")
    void deleteByBaseDateBefore(@Param("baseDate") LocalDate baseDate);
}
