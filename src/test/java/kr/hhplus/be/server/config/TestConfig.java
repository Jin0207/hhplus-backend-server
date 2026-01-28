package kr.hhplus.be.server.config;

import kr.hhplus.be.server.domain.message.MessageProducer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public MessageProducer testMessageProducer() {
        return new MessageProducer() {
            @Override
            public void send(String topic, String key, String payload) {
                // 테스트용 Mock 구현 - 아무것도 하지 않음
                System.out.println("Test MessageProducer: topic=" + topic + ", key=" + key);
            }
        };
    }
}
