package kr.hhplus.be.server.domain.message;

public interface MessageProducer {
    /**
     * @param topic   대상 시스템 (ORDER, PAYMENT 등)
     * @param key     식별자 (ID)
     * @param payload 실제 데이터 (JSON)
     */
    void send(String topic, String key, String payload);
}
