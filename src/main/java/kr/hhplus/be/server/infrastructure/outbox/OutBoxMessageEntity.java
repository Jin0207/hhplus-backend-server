package kr.hhplus.be.server.infrastructure.outbox;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "outbox_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class OutBoxMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "json", nullable = false)
    private String payload; 

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private boolean isProcessed = false;

    @Column(name = "processed_dttm")
    private LocalDateTime processedDttm;
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "crt_dttm", nullable = false, updatable = false)
    private LocalDateTime crtDttm;

    /**
     * 메시지 처리 완료 상태 변경
     */
    public void complete() {
        this.isProcessed = true;
    }

    /**
     * Domain → Entity
     */
    public static OutBoxMessageEntity from(OutBoxMessage domain) {
        return OutBoxMessageEntity.builder()
            .id(domain.id())
            .aggregateType(domain.aggregateType())
            .aggregateId(domain.aggregateId())
            .eventType(domain.eventType())
            .payload(domain.payload())
            .isProcessed(domain.isProcessed())
            .processedDttm(domain.processedDttm())
            .retryCount(domain.retryCount() != null ? domain.retryCount() : 0)
            .errorMessage(domain.errorMessage())
            .crtDttm(domain.crtDttm() != null ? domain.crtDttm() : LocalDateTime.now())
            .build();
    }

    /**
     * Entity → Domain
     */
    public OutBoxMessage toDomain() {
        return new OutBoxMessage(
            this.id,
            this.aggregateType,
            this.aggregateId,
            this.eventType,
            this.payload,
            this.isProcessed,
            this.processedDttm,
            this.retryCount,
            this.errorMessage,
            this.crtDttm
        );
    }
}
