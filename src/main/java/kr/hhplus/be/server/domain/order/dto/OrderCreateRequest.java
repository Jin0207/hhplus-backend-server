package kr.hhplus.be.server.domain.order.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 생성 요청")
public record OrderCreateRequest(
    @Schema(description = "사용자 ID", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다")
    Long userId,
    
    @Schema(description = "쿠폰 ID (선택)", example = "1")
    Long couponId,
    
    @Schema(description = "주문 상품 목록")
    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다")
    List<OrderItemRequest> items
) {}
