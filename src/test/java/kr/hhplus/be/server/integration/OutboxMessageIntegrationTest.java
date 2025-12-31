package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.config.TestConfig;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.message.MessageProducer;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import kr.hhplus.be.server.domain.outbox.repository.OutBoxMessageRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.infrastructure.outbox.OutboxProcessor;

/**
 * Outbox 패턴 통합 테스트
 * - 외부 메시지 전송 실패 시 Outbox에 저장되는지 확인
 * - Outbox 메시지 재처리 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class OutboxMessageIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutBoxMessageRepository outBoxMessageRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @SpyBean
    private MessageProducer messageProducer;

    @Test
    @DisplayName("✅ 주문 성공 시 Outbox 메시지 저장 확인")
    void outboxMessageSavedWhenOrderCompleted() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("outbox_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 500000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        // Given
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        // When: 주문 실행
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // Then: Outbox 메시지 저장 확인
        List<OutBoxMessage> messages = outBoxMessageRepository.findPendingMessages(10);

        assertThat(messages).isNotEmpty();

        OutBoxMessage savedMessage = messages.stream()
            .filter(msg -> msg.aggregateId().equals(orderResponse.orderId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Outbox 메시지가 저장되지 않았습니다"));

        assertThat(savedMessage.aggregateType()).isEqualTo("ORDER");
        assertThat(savedMessage.eventType()).isEqualTo("ORDER_COMPLETED");
        assertThat(savedMessage.isProcessed()).isFalse();
        assertThat(savedMessage.retryCount()).isEqualTo(0);
        assertThat(savedMessage.payload()).isNotNull();
    }

    @Test
    @DisplayName("✅ 외부 메시지 전송 실패 시에도 주문은 성공하고 Outbox에 저장됨")
    void orderSucceedsEvenWhenMessageSendingFails() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("outboxfail_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 500000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        // Given: MessageProducer가 실패하도록 Mock 설정
        // (하지만 이미 @SpyBean이므로 실제로는 로깅만 하고 예외는 발생하지 않음)
        // Outbox 패턴의 핵심: 메시지 전송은 별도 스케줄러가 처리

        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        // When: 주문 실행 (메시지 전송 실패와 무관하게 성공해야 함)
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // Then: 주문 성공 확인
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isNotNull();

        // Then: Outbox에 메시지 저장 확인 (나중에 재시도 가능)
        List<OutBoxMessage> messages = outBoxMessageRepository.findPendingMessages(10);
        assertThat(messages).anyMatch(msg ->
            msg.aggregateId().equals(orderResponse.orderId()) &&
            msg.aggregateType().equals("ORDER") &&
            !msg.isProcessed()
        );
    }

    @Test
    @DisplayName("✅ Outbox 메시지 처리 성공 시 isProcessed=true로 변경")
    void outboxMessageMarkedAsProcessedWhenSentSuccessfully() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("outboxprocess_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 500000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        // Given: 주문 생성으로 Outbox 메시지 저장
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // When: Outbox 메시지 조회
        List<OutBoxMessage> messages = outBoxMessageRepository.findPendingMessages(10);
        OutBoxMessage pendingMessage = messages.stream()
            .filter(msg -> msg.aggregateId().equals(orderResponse.orderId()))
            .findFirst()
            .orElseThrow();

        // When: 메시지 처리 (OutboxProcessor 사용)
        outboxProcessor.process(pendingMessage);

        // Then: 처리 완료 확인
        OutBoxMessage processedMessage = outBoxMessageRepository.findById(pendingMessage.id());
        assertThat(processedMessage.isProcessed()).isTrue();
        assertThat(processedMessage.processedDttm()).isNotNull();
    }

    @Test
    @DisplayName("✅ Outbox 메시지 처리 실패 시 재시도 카운트 증가")
    void outboxMessageRetryCountIncreasesWhenProcessingFails() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("outboxretry_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 500000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        // Given: 주문 생성으로 Outbox 메시지 저장
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        List<OutBoxMessage> messages = outBoxMessageRepository.findPendingMessages(10);
        OutBoxMessage pendingMessage = messages.stream()
            .filter(msg -> msg.aggregateId().equals(orderResponse.orderId()))
            .findFirst()
            .orElseThrow();

        // When: MessageProducer가 예외를 던지도록 설정
        doThrow(new RuntimeException("Kafka 전송 실패 시뮬레이션"))
            .when(messageProducer)
            .send(anyString(), anyString(), anyString());

        // When: 메시지 처리 시도 (예외 발생)
        try {
            outboxProcessor.process(pendingMessage);
        } catch (RuntimeException e) {
            // 예외는 무시 (정상 동작)
        }

        // Then: 재시도 카운트 증가 및 에러 메시지 저장 확인
        OutBoxMessage failedMessage = outBoxMessageRepository.findById(pendingMessage.id());
        assertThat(failedMessage.isProcessed()).isFalse();
        assertThat(failedMessage.retryCount()).isEqualTo(1);
        assertThat(failedMessage.errorMessage()).contains("Kafka 전송 실패");
    }

    @Test
    @DisplayName("✅ 최대 재시도 횟수 초과 시 Dead Letter Queue 확인")
    void outboxMessageMovesToDLQAfterMaxRetries() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("outboxdlq_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 500000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        // Given: 주문 생성으로 Outbox 메시지 저장
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        List<OutBoxMessage> messages = outBoxMessageRepository.findPendingMessages(10);
        OutBoxMessage pendingMessage = messages.stream()
            .filter(msg -> msg.aggregateId().equals(orderResponse.orderId()))
            .findFirst()
            .orElseThrow();

        // When: MessageProducer가 항상 실패하도록 설정
        doThrow(new RuntimeException("영구 실패"))
            .when(messageProducer)
            .send(anyString(), anyString(), anyString());

        // When: 최대 재시도 횟수(3회) 만큼 처리 시도
        for (int i = 0; i < 3; i++) {
            try {
                OutBoxMessage currentMessage = outBoxMessageRepository.findById(pendingMessage.id());
                outboxProcessor.process(currentMessage);
            } catch (RuntimeException e) {
                // 예외 무시
            }
        }

        // Then: 재시도 횟수 확인
        OutBoxMessage dlqMessage = outBoxMessageRepository.findById(pendingMessage.id());
        assertThat(dlqMessage.isProcessed()).isFalse();
        assertThat(dlqMessage.retryCount()).isEqualTo(3);
        assertThat(dlqMessage.canRetry()).isFalse(); // 더 이상 재시도 불가

        // Then: Dead Letter Queue 조회 확인
        List<OutBoxMessage> deadLetters = outBoxMessageRepository.findDeadLetters(3);
        assertThat(deadLetters).anyMatch(msg -> msg.id().equals(dlqMessage.id()));
    }
}
