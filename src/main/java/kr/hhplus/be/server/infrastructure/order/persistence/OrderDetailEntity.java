package kr.hhplus.be.server.infrastructure.order.persistence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_detail", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_product_id", columnList = "product_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderDetailEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Long subtotal;

    public static OrderDetailEntity from(OrderDetail orderDetail) {
        return OrderDetailEntity.builder()
            .id(orderDetail.id())
            .orderId(orderDetail.orderId())
            .productId(orderDetail.productId())
            .quantity(orderDetail.quantity())
            .unitPrice(orderDetail.unitPrice())
            .subtotal(orderDetail.subtotal())
            .build();
    }

    public OrderDetail toDomain() {
        return new OrderDetail(
            this.id,
            this.orderId,
            this.productId,
            this.quantity,
            this.unitPrice,
            this.subtotal,
            getCrtDttm(),
            getUpdDttm()
        );
    }
}