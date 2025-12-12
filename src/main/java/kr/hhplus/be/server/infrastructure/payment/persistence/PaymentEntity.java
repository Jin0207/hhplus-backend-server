package kr.hhplus.be.server.infrastructure.payment.persistence;
import java.time.LocalDateTime;

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
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name="idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payment_type", length = 20)
    private String paymentType;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "request_dttm")
    private LocalDateTime requestDttm;

    @Column(name = "success_dttm")
    private LocalDateTime successDttm;

    @Column(name = "external_sync", nullable = false)
    @Builder.Default
    private Boolean externalSync = false;

    @Column(name = "synced_dttm")
    private LocalDateTime syncedDttm;
}