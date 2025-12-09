package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CouponEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CouponType type;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "min_order_price", nullable = false)
    @Builder.Default
    private Integer minOrderPrice = 0;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    // Entity → Domain
    public Coupon toDomain() {
        return new Coupon(
            this.id,
            this.name,
            this.type,
            this.discountValue,
            this.minOrderPrice,
            this.validFrom,
            this.validTo,
            this.quantity,
            this.availableQuantity,
            this.status,
            this.getCrtDttm(),
            this.getUpdDttm()
        );
    }

    // Domain → Entity
    public static CouponEntity from(Coupon domain) {
        return CouponEntity.builder()
            .id(domain.id())
            .name(domain.name())
            .type(domain.type())
            .discountValue(domain.discountValue())
            .minOrderPrice(domain.minOrderPrice())
            .validFrom(domain.validFrom())
            .validTo(domain.validTo())
            .quantity(domain.quantity())
            .availableQuantity(domain.availableQuantity())
            .status(domain.status())
            .build();
    }
}
