package kr.hhplus.be.server.domain.outbox.repository;

import java.time.LocalDateTime;
import java.util.List;

import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;

public interface OutBoxMessageRepository {

    /**
     * 메시지 저장 및 업데이트
     */
    void save(OutBoxMessage message);

    /**
     * 처리되지 않은 대기 메시지 목록 조회
     * @param maxRetry 최대 재시도 횟수 제한
     * @return 대기 중인 도메인 메시지 리스트
     */
    List<OutBoxMessage> findPendingMessages(int maxRetry);

    /**
     * 재시도 횟수를 초과한 데드 레터(Dead Letter) 조회
     * @param maxRetry 재시도 임계치
     * @return 수동 처리가 필요한 메시지 리스트
     */
    List<OutBoxMessage> findDeadLetters(int maxRetry);

    /**
     * 오래된 처리 완료 메시지 일괄 삭제 (보관 정책 이행)
     * @param cutoffDate 삭제 기준 날짜 (이전 데이터 삭제)
     * @return 삭제된 건수
     */
    int deleteOldMessages(LocalDateTime cutoffDate);
    
    /**
     * 특정 ID로 메시지 조회 (필요 시)
     */
    OutBoxMessage findById(Long id);
}