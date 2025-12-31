package kr.hhplus.be.server.presentation.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import lombok.Builder;

@Schema(description = "상품 정보를 담고 있는 클래스")
@Builder
public record ProductResponse(
    @Schema(description = "상품 ID")
    Long id,
    @Schema(description = "상품명")
    String productName,
    @Schema(description = "가격")
    Long price,
    @Schema(description = "재고")
    Integer stock,
    @Schema(description = "카테고리코드")
    ProductCategory category,
    @Schema(description = "카테고리")
    String categoryDescription,
    @Schema(description = "상태코드")
    ProductStatus status,
    @Schema(description = "상태")
    String statusDescription,
    @Schema(description = "누적판매량")
    Integer salesQuantity
) {
    public static ProductResponse from(Product product){
        return ProductResponse.builder()
                .id(product.id())
                .productName(product.productName()) 
                .price(product.price()) 
                .stock(product.stock())
                .category(product.category()) 
                .categoryDescription(product.category().getDescription()) 
                .status(product.status())
                .statusDescription(product.status().getDescription())
                .salesQuantity(product.salesQuantity())
                .build();
    }

}