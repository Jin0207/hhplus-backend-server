package kr.hhplus.be.server.infrastructure.kafka;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 처리된 Kafka 이벤트 저장소
 *
 * Consumer의 멱등성 검증에 사용.
 * existsByEventId()로 이미 처리된 이벤트인지 확인하고 중복 처리를 방지한다.
 */
@Repository
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, Long> {

    Optional<ProcessedEventEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
