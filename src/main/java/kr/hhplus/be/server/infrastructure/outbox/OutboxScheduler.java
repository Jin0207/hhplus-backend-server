package kr.hhplus.be.server.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {
    private final OutBoxMessageJpaRepository outBoxMessageJpaRepository;
    private final OutboxProcessor outboxProcessor;

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    @Value("${outbox.max-retry:3}")
    private int maxRetry;

    @Value("${outbox.cleanup-days:7}")
    private int cleanupDays;
    
    /**
     * 5초마다 처리되지 않은 메시지 전송
     * - 재시도 횟수가 maxRetry 미만인 것만 처리
     */
    @Scheduled(fixedDelayString = "${outbox.schedule.publish-delay:5000}",
            initialDelayString = "${outbox.schedule.publish-initial-delay:10000}")
    public void publishPendingMessages() {
        try {
            List<OutBoxMessage> pendingMessages = outBoxMessageJpaRepository
                .findTop100ByIsProcessedFalseAndRetryCountLessThanOrderByCrtDttmAsc(maxRetry)
                .stream()
                .map(OutBoxMessageEntity::toDomain)
                .toList();
            
            if (pendingMessages.isEmpty()) {
                return;
            }
            
            log.info("[Outbox] 처리 시작: {} 건", pendingMessages.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (OutBoxMessage message : pendingMessages) {
                try {
                    outboxProcessor.process(message);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[Outbox] 처리 실패: id={}, retryCount={}", 
                        message.id(), message.retryCount(), e);
                }
            }
            
            log.info("[Outbox] 처리 완료: 성공={}, 실패={}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("[Outbox] 스케줄러 실행 중 오류", e);
        }
    }
    
    /**
     * 매일 새벽 3시: 오래된 처리 완료 메시지 삭제 (기본 7일 이상)
     */
    @Scheduled(cron = "${outbox.schedule.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupProcessedMessages() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
            int deletedCount = outBoxMessageJpaRepository.deleteProcessedMessagesBefore(cutoffDate);
            
            log.info("[Outbox] 정리 완료: {} 건 삭제", deletedCount);
            
        } catch (Exception e) {
            log.error("[Outbox] 정리 중 오류", e);
        }
    }
    
    /**
     * 매시간: 재시도 횟수 초과 메시지 알림 (Dead Letter Queue)
     */
    @Scheduled(cron = "${outbox.schedule.dlq-check-cron:0 0 * * * *}")
    public void checkDeadLetterQueue() {
        try {
            List<OutBoxMessageEntity> deadLetters = outBoxMessageJpaRepository
                .findByIsProcessedFalseAndRetryCountGreaterThanEqual(maxRetry);
            
            if (!deadLetters.isEmpty()) {
                log.warn("[Outbox] Dead Letter Queue: {} 건 - 수동 처리 필요", deadLetters.size());
                
                // 알림 전송 (추후 개발)
                deadLetters.forEach(msg -> 
                    log.error("[DLQ] aggregateType={}, aggregateId={}, error={}", 
                        msg.getAggregateType(), msg.getAggregateId(), msg.getErrorMessage())
                );
            }
            
        } catch (Exception e) {
            log.error("[Outbox] DLQ 체크 중 오류", e);
        }
    }

    
}
