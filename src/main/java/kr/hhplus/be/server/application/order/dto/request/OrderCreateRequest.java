package kr.hhplus.be.server.application.order.dto.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다")
    List<OrderItem> items,
    
    Long couponId,
    
    @Min(value = 0, message = "포인트는 0 이상이어야 합니다")
    Long pointToUse,
    
    @NotNull(message = "결제 수단은 필수입니다")
    String paymentType,

    @NotBlank(message = "멱등성 키는 필수입니다")
    @Size(min = 1, max = 100, message = "멱등성 키 길이를 확인해주세요")
    String idempotencyKey
) {
    
    public record OrderItem(
        @NotNull(message = "상품 ID는 필수입니다")
        Long productId,
        
        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        Integer quantity
    ) {}

}
