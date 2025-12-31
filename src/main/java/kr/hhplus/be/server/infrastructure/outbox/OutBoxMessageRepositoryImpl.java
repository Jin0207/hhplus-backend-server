package kr.hhplus.be.server.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import kr.hhplus.be.server.domain.outbox.repository.OutBoxMessageRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OutBoxMessageRepositoryImpl implements OutBoxMessageRepository {

    private final OutBoxMessageJpaRepository jpaRepository;

    @Override
    public void save(OutBoxMessage message) {
        jpaRepository.save(OutBoxMessageEntity.from(message));
    }

    @Override
    public List<OutBoxMessage> findPendingMessages(int maxRetry) {
        return jpaRepository.findTop100ByIsProcessedFalseAndRetryCountLessThanOrderByCrtDttmAsc(maxRetry)
                .stream()
                .map(OutBoxMessageEntity::toDomain)
                .toList();
    }

    @Override
    public List<OutBoxMessage> findDeadLetters(int maxRetry) {
        return jpaRepository.findByIsProcessedFalseAndRetryCountGreaterThanEqual(maxRetry)
                .stream()
                .map(OutBoxMessageEntity::toDomain)
                .toList();
    }

    @Override
    public int deleteOldMessages(LocalDateTime cutoffDate) {
        return jpaRepository.deleteProcessedMessagesBefore(cutoffDate);
    }

    @Override
    public OutBoxMessage findById(Long id) {
        return jpaRepository.findById(id)
                .map(OutBoxMessageEntity::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + id));
    }
}