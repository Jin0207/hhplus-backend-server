package kr.hhplus.be.server.infrastructure.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.infrastructure.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class ProductEntity extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "sales_quantity", nullable = false)
    private Integer salesQuantity;


    /**
     * Domain -> Entity 변환
     */
    public static ProductEntity from(Product product) {
        return ProductEntity.builder()
            .id(product.id())
            .productName(product.productName())
            .price(product.price())
            .stock(product.stock())
            .category(product.category())
            .status(product.status())
            .salesQuantity(product.salesQuantity())
            .build();
    }

    /**
     * Entity -> Domain 변환
     */
    public Product toDomain(){
        return new Product(
            this.id, 
            this.productName, 
            this.price, 
            this.stock, 
            this.category, 
            this.status, 
            this.salesQuantity, 
            getCrtDttm(), 
            getUpdDttm()
        );
    }
}