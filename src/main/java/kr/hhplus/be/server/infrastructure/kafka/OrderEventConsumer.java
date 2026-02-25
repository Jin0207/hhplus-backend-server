package kr.hhplus.be.server.infrastructure.kafka;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.infrastructure.external.DataPlatformClient;
import kr.hhplus.be.server.infrastructure.external.DataPlatformClient.OrderEventPayload;
import kr.hhplus.be.server.infrastructure.external.DataPlatformClient.OrderItemPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 완료 Kafka Consumer
 *
 * order.completed.v1 토픽을 수신하여 데이터 플랫폼으로 전송한다.
 *
 * 멱등성 보장:
 * - processed_order_events 테이블로 eventId 기반 중복 처리 방지
 * - Kafka의 at-least-once 특성으로 같은 메시지가 재전달될 수 있음
 *
 * 실패 처리:
 * - FAILED 상태로 processed_order_events에 기록
 * - 예외를 재발생시켜 Kafka가 재시도하도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final DataPlatformClient dataPlatformClient;
    private final ProcessedEventJpaRepository processedEventRepository;

    @KafkaListener(topics = OrderEventProducer.TOPIC, groupId = "order-consumer-group")
    @Transactional
    public void handleOrderCompleted(OrderEventMessage message) {
        String eventId = message.getEventId();
        Long orderId = message.getOrderId();

        try {
            log.info("Kafka 메시지 수신: eventId={}, orderId={}", eventId, orderId);

            // [멱등성 검증] 이미 처리된 eventId인지 확인
            if (processedEventRepository.existsByEventId(eventId)) {
                log.warn("이미 처리된 이벤트 - 스킵: eventId={}, orderId={}", eventId, orderId);
                return;
            }

            // 데이터 플랫폼 전송용 페이로드 구성
            List<OrderItemPayload> items = message.getItems().stream()
                    .map(item -> new OrderItemPayload(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal()
                    ))
                    .toList();

            OrderEventPayload payload = OrderEventPayload.of(
                    message.getOrderId(),
                    message.getUserId(),
                    message.getTotalAmount(),
                    message.getDiscountAmount(),
                    message.getFinalAmount(),
                    items
            );

            // 데이터 플랫폼으로 전송
            dataPlatformClient.sendOrderEvent(payload);

            // 처리 성공 기록 (이후 중복 수신 시 스킵)
            processedEventRepository.save(
                    ProcessedEventEntity.builder()
                            .eventId(eventId)
                            .orderId(orderId)
                            .status("SUCCESS")
                            .processedAt(LocalDateTime.now())
                            .build()
            );

            log.info("데이터 플랫폼 전송 완료: eventId={}, orderId={}", eventId, orderId);

        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: eventId={}, orderId={}, error={}",
                    eventId, orderId, e.getMessage(), e);

            // 실패 기록 저장 (미기록인 경우에만)
            try {
                if (!processedEventRepository.existsByEventId(eventId)) {
                    processedEventRepository.save(
                            ProcessedEventEntity.builder()
                                    .eventId(eventId)
                                    .orderId(orderId)
                                    .status("FAILED")
                                    .processedAt(LocalDateTime.now())
                                    .failureReason(e.getMessage())
                                    .build()
                    );
                }
            } catch (Exception dbEx) {
                log.error("실패 기록 저장 중 오류: eventId={}, error={}", eventId, dbEx.getMessage());
            }

            throw e; // Kafka 재시도를 위해 예외 재발생
        }
    }
}
