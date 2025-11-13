package kr.hhplus.be.server.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상세")
public record OrderDetailDto(
    @Schema(description = "상품 ID", example = "1")
    Long productId,
    
    @Schema(description = "상품명", example = "노트북")
    String productName,
    
    @Schema(description = "수량", example = "2")
    Integer quantity,
    
    @Schema(description = "소계", example = "3000000")
    Integer subtotal
) {}
