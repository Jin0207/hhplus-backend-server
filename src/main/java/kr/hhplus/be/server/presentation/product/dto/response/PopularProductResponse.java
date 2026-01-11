package kr.hhplus.be.server.presentation.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;

@Schema(description = "인기상품 조회 응답")
public record PopularProductResponse(
    @Schema(description = "상품 ID", example = "1")
    Long id,
    
    @Schema(description = "상품명", example = "노트북")
    String productName,
    
    @Schema(description = "가격", example = "1500000")
    Long price,
    
    @Schema(description = "재고수량", example = "50")
    Integer stock,
    
    @Schema(description = "카테고리", example = "전자제품")
    String category,
    
    @Schema(description = "판매 상태", example = "ON_SALE")
    ProductStatus status,
    
    @Schema(description = "누적판매량", example = "150")
    Integer salesQuantity
) {
    public static PopularProductResponse from(Product product) {
        return new PopularProductResponse(
            product.id(),
            product.productName(),
            product.price(),
            product.stock(),
            product.category().name(),
            product.status(),
            product.salesQuantity()
        );
    }
}
