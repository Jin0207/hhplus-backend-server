package kr.hhplus.be.server.infrastructure.outbox;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
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
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload; 

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private boolean isProcessed = false;

    @Column(name = "crt_dttm", nullable = false, updatable = false)
    private LocalDateTime crtDttm;

    /**
     * 메시지 처리 완료 상태 변경
     */
    public void complete() {
        this.isProcessed = true;
    }

    /**
     * Domain -> Entity
     */
    public static OutBoxMessageEntity from(OutBoxMessage domain) {
        return OutBoxMessageEntity.builder()
                .aggregateType(domain.aggregateType())
                .aggregateId(domain.aggregateId())
                .eventType(domain.eventType())
                .payload(domain.payload())
                .isProcessed(domain.isProcessed())
                .crtDttm(domain.crtDttm() != null ? domain.crtDttm() : LocalDateTime.now())
                .build();
    }
}
