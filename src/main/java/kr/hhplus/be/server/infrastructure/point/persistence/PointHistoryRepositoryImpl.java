package kr.hhplus.be.server.infrastructure.point.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository jpaRepository;

    @Override
    public PointHistory save(PointHistory pointHistory) {
        PointHistoryEntity entity = PointHistoryEntity.from(pointHistory);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<PointHistory> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(PointHistoryEntity::toDomain)
            .toList();
    }
}
