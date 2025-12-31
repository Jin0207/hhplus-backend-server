package kr.hhplus.be.server.infrastructure.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.enums.StockType;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class StockEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name="stock_type", nullable = false, length = 10)
    private StockType stockType;


    @Column(name = "reason", length=100)
    private String reason;


    /**
     * Domain -> Entity 변환
     */
    public static StockEntity from(Stock stock) {
        return StockEntity.builder()
            .id(stock.id())
            .productId(stock.productId())
            .quantity(stock.quantity())
            .stockType(stock.stockType())
            .reason(stock.reason())
            .build();
    }

    /**
     * Entity -> Domain 변환
     */
    public Stock toDomain(){
        return new Stock(
            this.id, 
            this.productId, 
            this.quantity, 
            this.stockType, 
            this.reason, 
            getCrtDttm()
        );
    }
}