package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.infrastructure.kafka.OrderEventMessage;
import kr.hhplus.be.server.infrastructure.kafka.OrderEventProducer;

/**
 * 주문 완료 이벤트 기반 데이터 플랫폼 연동 통합 테스트
 *
 * 기존 @TransactionalEventListener(AFTER_COMMIT) 직접 호출 방식에서
 * Kafka를 통한 비동기 전달 방식으로 변경.
 *
 * 검증 포인트:
 * 1. 주문 완료 시 Kafka 토픽으로 메시지 발행 (1회)
 * 2. Kafka 발행 실패해도 주문 트랜잭션은 롤백되지 않음
 * 3. 다중 상품 주문 시 하나의 Kafka 메시지에 모든 상품 정보 포함
 *
 * KafkaTemplate은 TestConfig에서 Mock으로 제공되므로 실제 Kafka 없이 테스트 가능.
 */
class DataPlatformIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void resetKafkaMock() {
        reset(kafkaTemplate);
    }

    @Test
    @DisplayName("주문 완료 시 Kafka 메시지 발행 확인")
    void order_completion_publishes_kafka_message_test() {
        // given
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("dataplatform_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 100000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                10000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        String idempotencyKey = UUID.randomUUID().toString();
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 2)),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        // when
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // then 1: 주문 상태 확인
        Order savedOrder = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(savedOrder.orderStatus()).isEqualTo(OrderStatus.COMPLETED);

        // then 2: 결제 상태 확인
        Payment savedPayment = paymentRepository.findById(orderResponse.payment().paymentId()).orElseThrow();
        assertThat(savedPayment.status()).isEqualTo(PaymentStatus.COMPLETED);

        // then 3: 포인트 차감 확인
        Long balanceAfterOrder = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterOrder).isEqualTo(80000L); // 100000 - 20000

        // [핵심 검증] Kafka 메시지가 order.completed.v1 토픽으로 1번 발행되었는지 확인
        // @TransactionalEventListener 방식에서 Kafka 발행 방식으로 변경된 것을 검증
        verify(kafkaTemplate, times(1)).send(
                eq(OrderEventProducer.TOPIC),
                anyString(),
                any(OrderEventMessage.class)
        );
    }

    @Test
    @DisplayName("Kafka 발행 실패해도 주문 트랜잭션은 롤백되지 않음")
    void kafka_publish_failure_does_not_rollback_transaction_test() {
        // given
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("kafka_fail_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 100000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품2",
                15000L,
                50,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        // [핵심] kafkaTemplate.send()가 예외를 발생시키도록 설정
        // afterCommit() 내부에서 catch되므로 도메인 트랜잭션에 영향 없음
        doThrow(new RuntimeException("Kafka 브로커 연결 실패"))
                .when(kafkaTemplate)
                .send(anyString(), anyString(), any());

        String idempotencyKey = UUID.randomUUID().toString();
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 2)),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        Long initialBalance = pointService.getPointBalance(testUser.id());

        // when: Kafka 발행 실패에도 불구하고 주문은 성공해야 함
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // then 1: [핵심] 주문은 정상적으로 완료됨
        // Kafka afterCommit 예외는 도메인 트랜잭션에 영향을 주지 않음
        Order savedOrder = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(savedOrder.orderStatus()).isEqualTo(OrderStatus.COMPLETED);

        // then 2: 결제도 정상 완료
        Payment savedPayment = paymentRepository.findById(orderResponse.payment().paymentId()).orElseThrow();
        assertThat(savedPayment.status()).isEqualTo(PaymentStatus.COMPLETED);

        // then 3: 재고 차감 확인
        Product updatedProduct = productRepository.findById(testProduct.id()).orElseThrow();
        assertThat(updatedProduct.stock()).isEqualTo(48); // 50 - 2

        // then 4: 포인트 차감 확인 (30000원 차감)
        Long balanceAfterOrder = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterOrder).isEqualTo(initialBalance - 30000L);

        // then 5: Kafka 발행 시도는 1회 발생함 (실패했어도)
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("다중 상품 주문 시 Kafka 메시지에 모든 상품 정보 포함 확인")
    void multiple_products_order_sends_single_kafka_message_with_all_items_test() {
        // given
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("multi_product_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 500000L, "테스트 충전");

        Product testProduct1 = productRepository.save(
            new Product(
                null,
                "노트북",
                200000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        Product testProduct2 = productRepository.save(
            new Product(
                null,
                "마우스",
                50000L,
                200,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );

        String idempotencyKey = UUID.randomUUID().toString();
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(
                new OrderCreateRequest.OrderItem(testProduct1.id(), 1), // 노트북 1개: 200,000원
                new OrderCreateRequest.OrderItem(testProduct2.id(), 2)  // 마우스 2개: 100,000원
            ),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        // when
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // then 1: 주문 완료 확인
        Order savedOrder = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(savedOrder.orderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(savedOrder.finalPrice()).isEqualTo(300000L); // 200,000 + 100,000

        // then 2: Kafka 메시지가 1번만 발행되었는지 확인
        verify(kafkaTemplate, times(1)).send(
                eq(OrderEventProducer.TOPIC),
                anyString(),
                any(OrderEventMessage.class)
        );

        // then 3: 발행된 메시지에 모든 상품 정보가 포함되어 있는지 확인
        ArgumentCaptor<OrderEventMessage> messageCaptor = ArgumentCaptor.forClass(OrderEventMessage.class);
        verify(kafkaTemplate).send(eq(OrderEventProducer.TOPIC), anyString(), messageCaptor.capture());

        OrderEventMessage publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage.getOrderId()).isEqualTo(savedOrder.id());
        assertThat(publishedMessage.getUserId()).isEqualTo(testUser.id());
        assertThat(publishedMessage.getFinalAmount()).isEqualTo(300000L);
        assertThat(publishedMessage.getItems()).hasSize(2);
    }
}
