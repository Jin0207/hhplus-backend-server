package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxFailureHandler {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final OutBoxMessageJpaRepository outBoxMessageJpaRepository;

    /**
     * 실패 처리: 재시도 카운트 증가 및 에러 메시지 저장 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(OutBoxMessage message, Exception e) {
        try {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > MAX_ERROR_MESSAGE_LENGTH) {
                errorMsg = errorMsg.substring(0, MAX_ERROR_MESSAGE_LENGTH);
            }

            OutBoxMessage failed = message.incrementRetry(errorMsg);
            OutBoxMessageEntity entity = OutBoxMessageEntity.from(failed);
            outBoxMessageJpaRepository.save(entity);

            log.warn("[Outbox] 전송 실패 (재시도: {}): aggregateId={}, error={}",
                failed.retryCount(), message.aggregateId(), errorMsg);

            // 재시도 불가능한 경우 알림
            if (!failed.canRetry()) {
                log.error("[Outbox] 최대 재시도 초과: aggregateId={} - Dead Letter Queue로 이동",
                    message.aggregateId());
            }

        } catch (Exception ex) {
            log.error("[Outbox] 실패 처리 중 오류: aggregateId={}", message.aggregateId(), ex);
        }
    }
}
