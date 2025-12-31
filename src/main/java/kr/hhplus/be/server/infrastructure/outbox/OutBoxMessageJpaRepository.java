package kr.hhplus.be.server.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutBoxMessageJpaRepository extends JpaRepository<OutBoxMessageEntity, Long> {
    
    List<OutBoxMessageEntity> findTop100ByIsProcessedFalseAndRetryCountLessThanOrderByCrtDttmAsc(int maxRetry);
    
    /**
     * 오래된 처리 완료 메시지 삭제 (7일 이상)
     */
    @Modifying
    @Query("DELETE FROM OutBoxMessageEntity o WHERE o.isProcessed = true AND o.processedDttm < :cutoffDate")
    int deleteProcessedMessagesBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 재시도 횟수 초과 메시지 조회 (Dead Letter Queue)
     */
    List<OutBoxMessageEntity> findByIsProcessedFalseAndRetryCountGreaterThanEqual(int maxRetry);
    
    /**
     * 특정 집계 타입의 미처리 메시지 수 조회 (모니터링용)
     */
    long countByAggregateTypeAndIsProcessedFalse(String aggregateType);
    
    /**
     * 재시도 횟수별 통계 (모니터링용)
     */
    @Query("""
        SELECT o.retryCount, COUNT(o) 
        FROM OutBoxMessageEntity o 
        WHERE o.isProcessed = false 
        GROUP BY o.retryCount
        """)
    List<Object[]> getRetryCountStatistics();
}