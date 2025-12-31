package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.message.MessageProducer;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final MessageProducer messageProducer;
    private final OutBoxMessageJpaRepository outBoxMessageJpaRepository;
    private final OutboxFailureHandler failureHandler;

    /**
     * 메시지 처리 (성공 시 완료, 실패 시 재시도 카운트 증가)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(OutBoxMessage message) {
        try {
            // 1. 메시지 전송
            messageProducer.send(
                message.aggregateType(),
                String.valueOf(message.aggregateId()),
                message.payload()
            );

            // 2. 성공 시 완료 처리
            OutBoxMessage completed = message.complete();
            OutBoxMessageEntity entity = OutBoxMessageEntity.from(completed);
            outBoxMessageJpaRepository.save(entity);

            log.info("[Outbox] 전송 성공: aggregateType={}, aggregateId={}",
                message.aggregateType(), message.aggregateId());

        } catch (Exception e) {
            // 3. 실패 시 재시도 카운트 증가 (별도 트랜잭션)
            failureHandler.handleFailure(message, e);
            throw e; // 예외 재발생
        }
    }
}