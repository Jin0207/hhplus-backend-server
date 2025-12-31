package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import kr.hhplus.be.server.application.order.dto.response.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventOutboxListener {
    private final OutboxMessageManager outboxMessageManager;

    /**
     * 주문 트랜잭션 커밋 직전에 Outbox 테이블에 기록
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        outboxMessageManager.save(
            "ORDER", 
            event.order().id(), 
            "ORDER_COMPLETED", 
            event
        );
    }
}