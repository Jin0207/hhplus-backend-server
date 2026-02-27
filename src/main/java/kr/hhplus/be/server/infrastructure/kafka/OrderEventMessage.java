package kr.hhplus.be.server.infrastructure.kafka;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 완료 Kafka 메시지
 *
 * 주문 완료 시 데이터 플랫폼으로 전달할 정보를 담는 메시지 객체.
 * - eventId: 멱등성 검증용 UUID (Consumer에서 중복 처리 방지)
 * - orderId: 메시지 Key로 사용 (같은 주문은 같은 partition)
 * - occurredAt: ISO-8601 형식 문자열 (직렬화 안정성)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage {

    private String eventId;
    private Long orderId;
    private Long userId;
    private Long totalAmount;
    private Long discountAmount;
    private Long finalAmount;
    private List<OrderItemMessage> items;
    private String occurredAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemMessage {
        private Long productId;
        private Integer quantity;
        private Long unitPrice;
        private Long subtotal;
    }
}
