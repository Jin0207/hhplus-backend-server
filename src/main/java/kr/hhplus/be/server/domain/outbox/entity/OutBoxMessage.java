package kr.hhplus.be.server.domain.outbox.entity;

import java.time.LocalDateTime;

public record OutBoxMessage(
    Long  id,               // 식별자
    String  aggregateType,  // 집계타입
    Long  aggregateId,      // 집계식별자
    String eventType,       // 이벤트타입
    String payload,         // 전송데이터(JSON)
    boolean isProcessed,    // 처리여부
    LocalDateTime crtDttm   // 생성일
) {
    

}
