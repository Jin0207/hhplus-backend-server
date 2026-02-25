package kr.hhplus.be.server.infrastructure.external;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.hhplus.be.server.application.order.dto.response.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.infrastructure.external.DataPlatformClient.OrderEventPayload;
import kr.hhplus.be.server.infrastructure.external.DataPlatformClient.OrderItemPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 완료 이벤트 리스너 - 데이터 플랫폼 전송
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)로 표시하여
 * 트랜잭션 커밋 이후에만 실행됩니다.
 *
 * 역할:
 * 1. 데이터 플랫폼에 주문 정보 전송
 * 2. 외부 API 호출 실패가 도메인 로직에 영향을 주지 않음
 * 3. 실패를 로그로 남겨 추적 가능하게 함 (eventId 기반)
 *
 * 이를 통해:
 * - 트랜잭션과 외부 호출의 완벽한 분리
 * - 관심사의 분리 (도메인 로직 ↔ 데이터 연동 로직)
 * - 외부 시스템 장애가 핵심 비즈니스에 영향을 주지 않음
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataPlatformEventListener {

        private final DataPlatformClient dataPlatformClient;

        /**
         * 주문 완료 이벤트 처리
         *
         * @TransactionalEventListener(phase = AFTER_COMMIT):
         * - 트랜잭션이 성공적으로 커밋된 후에만 호출됨
         * - 롤백된 경우 이 메서드는 호출되지 않음
         *
         * @param event 주문 완료 이벤트
         */
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleOrderCompleted(OrderCompletedEvent event) {
        String eventId = UUID.randomUUID().toString();
        Order order = event.order();

        try {
                log.info("주문 완료 이벤트 처리 시작: eventId={}, orderId={}",
                        eventId, order.id());

                // 데이터 플랫폼 전송용 페이로드 구성
                List<OrderItemPayload> items = event.orderDetails().stream()
                        .map(this::toItemPayload)
                        .toList();

                OrderEventPayload payload = OrderEventPayload.of(
                        order.id(),
                        order.userId(),
                        order.totalPrice(),
                        order.discountPrice(),
                        order.finalPrice(),
                        items
                );

                // 데이터 플랫폼으로 전송
                dataPlatformClient.sendOrderEvent(payload);

                log.info("주문 완료 이벤트 처리 완료: eventId={}, orderId={}",
                        eventId, order.id());

        } catch (Exception e) {
                // 외부 API 호출 실패 시:
                // - 도메인 트랜잭션은 이미 커밋됨 (롤백되지 않음)
                // - 실패는 로그로 남겨짐 (eventId로 추적 가능)
                log.error("주문 완료 이벤트 처리 실패: eventId={}, orderId={}, error={}",
                        eventId, order.id(), e.getMessage(), e);
        }
        }

        private OrderItemPayload toItemPayload(OrderDetail detail) {
        return new OrderItemPayload(
                detail.productId(),
                detail.quantity(),
                detail.unitPrice(),
                detail.subtotal()
        );
        }
}
