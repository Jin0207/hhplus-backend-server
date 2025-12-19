package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.application.order.dto.response.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventOutboxListener {
    private final OutboxMessageManager outboxMessageManager;

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Outbox 기록 시작: orderId={}", event.order().id());
        
        outboxMessageManager.save(
            "ORDER",
            event.order().id(),
            "ORDER_COMPLETED",
            event
        );
    }
}