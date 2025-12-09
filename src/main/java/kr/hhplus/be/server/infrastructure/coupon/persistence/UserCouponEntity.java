package kr.hhplus.be.server.infrastructure.coupon.persistence;

import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@IdClass(UserCouponId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserCouponEntity extends BaseTimeEntity{
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;

    @Column(name = "used_dttm")
    private LocalDateTime usedDttm;

    @Column(name = "expired_dttm")
    private LocalDateTime expiredDttm;

    // Entity → Domain
    public UserCoupon toDomain() {
        return new UserCoupon(
            this.userId,
            this.couponId,
            this.status,
            this.usedDttm,
            this.expiredDttm,
            this.getCrtDttm(),
            this.getUpdDttm()
        );
    }

    // Domain → Entity
    public static UserCouponEntity from(UserCoupon domain) {
        return UserCouponEntity.builder()
            .userId(domain.userId())
            .couponId(domain.couponId())
            .status(domain.status())
            .usedDttm(domain.usedDttm())
            .expiredDttm(domain.expiredDttm())
            .build();
    }
}
