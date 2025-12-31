package kr.hhplus.be.server.infrastructure.payment.persistence;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
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

    public static PaymentEntity from(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.id())
                .orderId(payment.orderId())
                .userId(payment.userId())
                .idempotencyKey(payment.idempotencyKey())
                .price(payment.price() != null ? payment.price().intValue() : 0)
                .status(payment.status())
                .paymentType(payment.paymentType() != null ? payment.paymentType().name() : null)
                .paymentGateway(payment.paymentGateway())
                .transactionId(payment.transactionId())
                .failReason(payment.failReason())
                .requestDttm(payment.requestDttm())
                .successDttm(payment.successDttm())
                .externalSync(payment.externalSync() != null ? payment.externalSync() : false)
                .syncedDttm(payment.syncedDttm())
                .build();
    }

    public Payment toDomain() {
        return new Payment(
                this.id,
                this.orderId,
                this.userId,
                this.idempotencyKey,
                this.price != null ? this.price.longValue() : 0L,
                this.status,
                this.paymentType != null ? PaymentType.valueOf(this.paymentType) : null,
                this.paymentGateway,
                this.transactionId,
                this.failReason,
                this.requestDttm,
                this.successDttm,
                this.externalSync,
                this.syncedDttm,
                this.getCrtDttm(),
                this.getUpdDttm()
        );
    }
}