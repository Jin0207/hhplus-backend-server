package kr.hhplus.be.server.infrastructure.point.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long>{
    List<PointHistoryEntity> findByUserId(Long userId);
}
