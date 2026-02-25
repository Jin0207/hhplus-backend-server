package kr.hhplus.be.server.infrastructure.external;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 플랫폼 외부 API 호출 클라이언트 (Mock)
 *
 * 실시간 주문정보를 데이터 플랫폼에 전송하는 역할을 합니다.
 * 현재는 Mock 구현으로, 실제 HTTP 호출을 시뮬레이션합니다.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)과 함께 사용하여
 * 트랜잭션 커밋 후에만 외부 호출이 발생하도록 합니다.
 */
@Component
@Slf4j
public class DataPlatformClient {

    /**
     * 주문 완료 이벤트를 데이터 플랫폼에 전송
     *
     * @param payload 전송할 주문 정보 페이로드
     */
    public void sendOrderEvent(OrderEventPayload payload) {
        try {
            // 외부 API 호출 시뮬레이션 (지연 추가)
            simulateNetworkDelay();

            // Mock API 호출 (실제로는 HTTP 요청)
            log.info("데이터 플랫폼에 주문정보 전송: orderId={}, userId={}, totalAmount={}, itemCount={}",
                    payload.orderId(),
                    payload.userId(),
                    payload.totalAmount(),
                    payload.items().size());

            // Mock 성공 응답
            if (payload.totalAmount() < 0) {
                throw new RuntimeException("Invalid order amount");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("데이터 플랫폼 전송 중 중단됨", e);
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: orderId={}", payload.orderId(), e);
            throw new RuntimeException("데이터 플랫폼 전송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 네트워크 지연 시뮬레이션
     */
    private void simulateNetworkDelay() throws InterruptedException {
        // 100ms의 외부 API 호출 지연을 시뮬레이션
        Thread.sleep(100);
    }

    /**
     * 주문 이벤트 페이로드
     */
    public record OrderEventPayload(
        Long orderId,
        Long userId,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        List<OrderItemPayload> items,
        LocalDateTime occurredAt
    ) {
        public static OrderEventPayload of(
                Long orderId,
                Long userId,
                Long totalAmount,
                Long discountAmount,
                Long finalAmount,
                List<OrderItemPayload> items) {
            return new OrderEventPayload(
                orderId, userId, totalAmount, discountAmount, finalAmount, items, LocalDateTime.now()
            );
        }
    }

    /**
     * 주문 상품 페이로드
     */
    public record OrderItemPayload(
        Long productId,
        Integer quantity,
        Long unitPrice,
        Long subtotal
    ) {}
}
