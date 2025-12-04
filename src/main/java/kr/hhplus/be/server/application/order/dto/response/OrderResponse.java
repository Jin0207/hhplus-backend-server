package kr.hhplus.be.server.application.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import lombok.Builder;

@Builder
public record OrderResponse(
    Long orderId,
    Long userId,
    String orderStatus,
    Integer totalPrice,
    Integer discountPrice,
    Integer finalPrice,
    PaymentInfo payment,
    List<OrderItemInfo> items,
    LocalDateTime createdAt
) {
    @Builder
    public record PaymentInfo(
        Long paymentId,
        String status,
        PaymentType paymentType,
        String transactionId
    ) {}
    
    @Builder
    public record OrderItemInfo(
        Long productId,
        Integer quantity,
        Integer unitPrice,
        Integer subtotal
    ) {}
    
    public static OrderResponse from(
            Order order,
            Payment payment,
            List<OrderDetail> orderDetails) {
        
        PaymentInfo paymentInfo = PaymentInfo.builder()
            .paymentId(payment.id())
            .status(payment.status().name())
            .paymentType(payment.paymentType())
            .transactionId(payment.transactionId())
            .build();
        
        List<OrderItemInfo> items = orderDetails.stream()
            .map(detail -> OrderItemInfo.builder()
                .productId(detail.productId())
                .quantity(detail.quantity())
                .unitPrice(detail.unitPrice())
                .subtotal(detail.subtotal())
                .build())
            .toList();
        
        return OrderResponse.builder()
            .orderId(order.id())
            .userId(order.userId())
            .orderStatus(order.orderStatus().name())
            .totalPrice(order.totalPrice())
            .discountPrice(order.discountPrice())
            .finalPrice(order.finalPrice())
            .payment(paymentInfo)
            .items(items)
            .createdAt(order.crtDttm())
            .build();
    }
}