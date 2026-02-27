package kr.hhplus.be.server.infrastructure.kafka;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 처리된 Kafka 이벤트 기록
 *
 * Kafka는 at-least-once 보장이므로 같은 메시지가 여러 번 전달될 수 있다.
 * Consumer가 eventId 기반으로 중복 처리를 방지하기 위해 이 테이블에 기록한다.
 *
 * UK(event_id): 같은 eventId는 1회만 저장 가능 → DB 레벨 멱등성 보장
 */
@Entity
@Table(
    name = "processed_order_events",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "event_id", name = "uk_processed_order_events_event_id")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kafka 메시지의 eventId (UUID) - 중복 처리 방지 키 */
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    /** 처리된 주문 ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 처리 결과: SUCCESS / FAILED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /** 실패 사유 (실패 시에만 기록) */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
