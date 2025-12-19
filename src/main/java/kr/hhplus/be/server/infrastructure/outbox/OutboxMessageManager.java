package kr.hhplus.be.server.infrastructure.outbox;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.hhplus.be.server.domain.outbox.repository.OutBoxMessageRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxMessageManager {
    private final OutBoxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(String aggregateType, Long aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            OutBoxMessageEntity entity = OutBoxMessageEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonPayload)
                .isProcessed(false)
                .crtDttm(LocalDateTime.now())
                .build();
                
            outboxMessageRepository.save(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox 실패", e);
        }
    }
}