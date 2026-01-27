package kr.hhplus.be.server.infrastructure.message;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.hhplus.be.server.domain.message.MessageProducer;
import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 개발 및 테스트용 No-Op MessageProducer
 * 실제 메시지를 전송하지 않고 로그만 출력
 */
@Slf4j
@Component
@Profile({"local", "test"})
public class NoOpMessageProducer implements MessageProducer {

    @Override
    public void send(String topic, String key, String payload) {
        log.info("NoOpMessageProducer - Message would be sent: topic={}, key={}, payload={}",
            topic, key, payload);
    }
}
