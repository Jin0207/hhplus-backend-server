package kr.hhplus.be.server.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 응답")
public record OrderResponse(
    @Schema(description = "주문 ID", example = "1")
    Long orderId,
    
    @Schema(description = "사용자 ID", example = "1")
    Long userId,
    
    @Schema(description = "총 주문 금액", example = "3000000")
    Integer totalPrice,
    
    @Schema(description = "할인 금액", example = "300000")
    Integer discountPrice,
    
    @Schema(description = "최종 결제 금액", example = "2700000")
    Integer finalPrice,
    
    @Schema(description = "주문 상태", example = "PENDING")
    String orderStatus,
    
    @Schema(description = "주문 상세 목록")
    List<OrderDetailDto> orderDetails,
    
    @Schema(description = "주문 생성일시")
    LocalDateTime crtDttm
) {}