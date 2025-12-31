package kr.hhplus.be.server.domain.outbox.entity;

import java.time.LocalDateTime;

public record OutBoxMessage(
    Long  id,               // 식별자
    String  aggregateType,  // 집계타입
    Long  aggregateId,      // 집계식별자
    String eventType,       // 이벤트타입
    String payload,         // 전송데이터(JSON)
    boolean isProcessed,    // 처리여부
    LocalDateTime processedDttm,  // 처리 시각
    Integer retryCount,            // 재시도 횟수 
    String errorMessage,           // 에러 메시지
    LocalDateTime crtDttm   // 생성일
) {
    /**
     * 처리 완료 상태로 변경
     */
    public OutBoxMessage complete() {
        return new OutBoxMessage(
            id, 
            aggregateType, 
            aggregateId, 
            eventType, 
            payload, 
            true,                      // isProcessed = true
            LocalDateTime.now(),       // 처리 시각 기록
            retryCount, 
            null,                      // 에러 메시지 초기화
            crtDttm
        );
    }
    /**
     * 실패 시 재시도 횟수 증가
     */
    public OutBoxMessage incrementRetry(String error) {
        return new OutBoxMessage(
            id,
            aggregateType,
            aggregateId,
            eventType,
            payload,
            false,
            null,
            (retryCount != null ? retryCount : 0) + 1,
            error,
            crtDttm
        );
    }
    
    /**
     * 재시도 가능 여부 (최대 3회)
     */
    public boolean canRetry() {
        return (retryCount == null ? 0 : retryCount) < 3;
    }

    /**
     * 생성 팩토리 메서드
     */
    public static OutBoxMessage create(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload) {
        return new OutBoxMessage(
            null,
            aggregateType,
            aggregateId,
            eventType,
            payload,
            false,
            null,
            0,
            null,
            LocalDateTime.now()
        );
    }
}
