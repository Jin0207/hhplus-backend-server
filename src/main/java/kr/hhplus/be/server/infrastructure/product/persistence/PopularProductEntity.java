package kr.hhplus.be.server.infrastructure.product.persistence;

import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;
import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "popular_products",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_base_date_rank", columnNames = {"base_date", "`rank`"})
    },
    indexes = {
        @Index(name = "idx_base_date", columnList = "base_date")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class PopularProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`rank`", nullable = false)
    private Integer rank;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ProductCategory category;

    @Column(name = "total_sales_quantity", nullable = false)
    private Integer totalSalesQuantity;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "crt_dttm", nullable = false, updatable = false)
    private LocalDateTime crtDttm;

    /**
     * Domain -> Entity 변환
     */
    public static PopularProductEntity from(PopularProduct popularProduct) {
        return PopularProductEntity.builder()
            .id(popularProduct.id())
            .rank(popularProduct.rank())
            .productId(popularProduct.productId())
            .productName(popularProduct.productName())
            .price(popularProduct.price())
            .category(popularProduct.category())
            .totalSalesQuantity(popularProduct.totalSalesQuantity())
            .baseDate(popularProduct.baseDate())
            .periodStartDate(popularProduct.periodStartDate())
            .periodEndDate(popularProduct.periodEndDate())
            .crtDttm(popularProduct.crtDttm() != null ? popularProduct.crtDttm() : LocalDateTime.now())
            .build();
    }

    /**
     * Entity -> Domain 변환
     */
    public PopularProduct toDomain() {
        return new PopularProduct(
            this.id,
            this.rank,
            this.productId,
            this.productName,
            this.price,
            this.category,
            this.totalSalesQuantity,
            this.baseDate,
            this.periodStartDate,
            this.periodEndDate,
            this.crtDttm
        );
    }
}
