package kr.hhplus.be.server.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 완료 Kafka Producer
 *
 * TransactionSynchronizationManager를 활용하여 트랜잭션 커밋 이후에만 메시지를 발행한다.
 *
 * 이를 통해:
 * 1. 롤백 시 메시지 발행 방지 (정합성 보장)
 * 2. 메시지 발행 실패가 도메인 트랜잭션에 영향 없음
 * 3. 커밋 이후 발행으로 DB와 Kafka 간 순서 보장
 *
 * Topic: order.completed.v1 (버전 포함 - 스키마 변경 시 v2로 확장 가능)
 * Key: orderId (같은 주문은 같은 partition으로 보장)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String TOPIC = "order.completed.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 주문 완료 이벤트를 Kafka로 발행 (트랜잭션 커밋 이후 보장)
     *
     * @param message 주문 이벤트 메시지
     */
    public void publishOrderCompletedAfterCommit(OrderEventMessage message) {
        try {
            log.info("Kafka 메시지 발행 등록 (커밋 이후): eventId={}, orderId={}, topic={}",
                    message.getEventId(), message.getOrderId(), TOPIC);

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                String key = message.getOrderId().toString();
                                kafkaTemplate.send(TOPIC, key, message);

                                log.info("Kafka 메시지 발행 완료 (커밋 이후): eventId={}, orderId={}",
                                        message.getEventId(), message.getOrderId());
                            } catch (Exception e) {
                                log.error("Kafka 메시지 발행 실패 (커밋 이후): eventId={}, orderId={}, error={}",
                                        message.getEventId(), message.getOrderId(), e.getMessage(), e);
                                // afterCommit에서의 예외는 도메인 트랜잭션에 영향 없음
                            }
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                log.warn("트랜잭션 롤백 - Kafka 메시지 발행 취소: eventId={}, orderId={}",
                                        message.getEventId(), message.getOrderId());
                            }
                        }
                    }
            );

            log.debug("Kafka 메시지 발행 콜백 등록 완료: eventId={}", message.getEventId());
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 콜백 등록 실패: eventId={}, orderId={}",
                    message.getEventId(), message.getOrderId(), e);
            throw e;
        }
    }
}
