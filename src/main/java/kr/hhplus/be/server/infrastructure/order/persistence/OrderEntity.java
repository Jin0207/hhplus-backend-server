package kr.hhplus.be.server.infrastructure.order.persistence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "discount_price", nullable = false)
    @Builder.Default
    private Long discountPrice = 0L;

    @Column(name = "final_price", nullable = false)
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus;

    /**
     * Entity -> Domain 변환
     */
    public Order toDomain() {
        return new Order(
            this.id,
            this.userId,
            this.couponId,
            this.totalPrice,
            this.discountPrice,
            this.finalPrice,
            this.orderStatus,
            this.getCrtDttm(),
            this.getUpdDttm()
        );
    }

    /**
     * Domain -> Entity 변환
     */
    public static OrderEntity from(Order order) {
        return OrderEntity.builder()
            .id(order.id())
            .userId(order.userId())
            .couponId(order.couponId())
            .totalPrice(order.totalPrice())
            .discountPrice(order.discountPrice())
            .finalPrice(order.finalPrice())
            .orderStatus(order.orderStatus())
            .build();
    }
}