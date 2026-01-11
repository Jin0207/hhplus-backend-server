package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.config.TestConfig;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import kr.hhplus.be.server.domain.outbox.repository.OutBoxMessageRepository;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;

/**
 * 주문 → 재고 차감 → 잔액 차감 → 주문 저장 → Outbox 저장 전체 플로우 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class OrderPaymentFlowIntegrationTest {

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
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutBoxMessageRepository outBoxMessageRepository;

    @Test
    @DisplayName("✅ 주문 → 재고 차감 → 잔액 차감 → 주문 저장 → Outbox 저장 전체 플로우")
    void completeOrderFlowWithAllSteps() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("orderflow_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 600000L, "테스트 충전"); // 550,000원 주문이므로 600,000원 충전

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

        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(
                new OrderCreateRequest.OrderItem(testProduct1.id(), 2), // 노트북 2개
                new OrderCreateRequest.OrderItem(testProduct2.id(), 3)  // 마우스 3개
            ),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        Long initialBalance = pointService.getPointBalance(testUser.id());
        Long expectedTotalPrice = (200000L * 2) + (50000L * 3); // 550,000원

        // When: 주문 실행
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // Then 1: 주문 응답 검증
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isNotNull();
        assertThat(orderResponse.totalPrice()).isEqualTo(expectedTotalPrice);
        assertThat(orderResponse.finalPrice()).isEqualTo(expectedTotalPrice);

        // Then 2: 주문 DB 저장 확인
        Order savedOrder = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(savedOrder.orderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(savedOrder.userId()).isEqualTo(testUser.id());
        assertThat(savedOrder.finalPrice()).isEqualTo(expectedTotalPrice);

        // Then 3: 주문 상세 DB 저장 확인
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(savedOrder.id());
        assertThat(orderDetails).hasSize(2);
        assertThat(orderDetails).extracting(OrderDetail::productId)
            .containsExactlyInAnyOrder(testProduct1.id(), testProduct2.id());
        assertThat(orderDetails).extracting(OrderDetail::quantity)
            .containsExactlyInAnyOrder(2, 3);

        // Then 4: 재고 차감 확인
        Product updatedProduct1 = productRepository.findById(testProduct1.id()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(testProduct2.id()).orElseThrow();
        assertThat(updatedProduct1.stock()).isEqualTo(98);  // 100 - 2
        assertThat(updatedProduct2.stock()).isEqualTo(197); // 200 - 3

        // Then 5: 판매량 증가 확인
        assertThat(updatedProduct1.salesQuantity()).isEqualTo(2);
        assertThat(updatedProduct2.salesQuantity()).isEqualTo(3);

        // Then 6: 잔액 차감 확인
        Long balanceAfterOrder = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterOrder).isEqualTo(initialBalance - expectedTotalPrice);

        // Then 7: 결제 DB 저장 확인
        Payment savedPayment = paymentRepository.findById(orderResponse.payment().paymentId()).orElseThrow();
        assertThat(savedPayment.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(savedPayment.orderId()).isEqualTo(savedOrder.id());
        assertThat(savedPayment.userId()).isEqualTo(testUser.id());
        assertThat(savedPayment.idempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(savedPayment.price()).isEqualTo(expectedTotalPrice);

        // Then 8: Outbox 메시지 저장 확인
        List<OutBoxMessage> outboxMessages = outBoxMessageRepository.findPendingMessages(10);
        assertThat(outboxMessages).isNotEmpty();
        assertThat(outboxMessages).anyMatch(msg ->
            msg.aggregateType().equals("ORDER") &&
            msg.aggregateId().equals(savedOrder.id()) &&
            msg.eventType().equals("ORDER_COMPLETED") &&
            !msg.isProcessed()
        );
    }

    @Test
    @DisplayName("✅ 재고 부족 시 전체 롤백 확인")
    void rollbackWhenInsufficientStock() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("rollback_" + timestamp + "@example.com", "password123"));
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

        // Given: 재고보다 많은 수량 주문
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(
                new OrderCreateRequest.OrderItem(testProduct1.id(), 150) // 재고 100개인데 150개 주문
            ),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        Long initialBalance = pointService.getPointBalance(testUser.id());
        Integer initialStock = testProduct1.stock();

        // When & Then: 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            kr.hhplus.be.server.support.exception.BusinessException.class,
            () -> orderFacade.completeOrder(testUser.id(), orderRequest)
        );

        // Then: 잔액 롤백 확인
        Long balanceAfterFailed = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterFailed).isEqualTo(initialBalance);

        // Then: 재고 롤백 확인
        Product unchangedProduct = productRepository.findById(testProduct1.id()).orElseThrow();
        assertThat(unchangedProduct.stock()).isEqualTo(initialStock);
        assertThat(unchangedProduct.salesQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("✅ 다중 상품 주문 시 부분 실패하면 전체 롤백")
    void rollbackAllWhenOneProductFails() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("multirollback_" + timestamp + "@example.com", "password123"));
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

        // Given: 첫 번째 상품은 재고 충분, 두 번째 상품은 재고 부족
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(
                new OrderCreateRequest.OrderItem(testProduct1.id(), 2),  // 재고 충분 (100개)
                new OrderCreateRequest.OrderItem(testProduct2.id(), 250) // 재고 부족 (200개)
            ),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        Long initialBalance = pointService.getPointBalance(testUser.id());
        Integer initialStock1 = testProduct1.stock();
        Integer initialStock2 = testProduct2.stock();

        // When & Then: 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            kr.hhplus.be.server.support.exception.BusinessException.class,
            () -> orderFacade.completeOrder(testUser.id(), orderRequest)
        );

        // Then: 모든 재고 롤백 확인 (첫 번째 상품도 차감되지 않음)
        Product unchangedProduct1 = productRepository.findById(testProduct1.id()).orElseThrow();
        Product unchangedProduct2 = productRepository.findById(testProduct2.id()).orElseThrow();
        assertThat(unchangedProduct1.stock()).isEqualTo(initialStock1);
        assertThat(unchangedProduct2.stock()).isEqualTo(initialStock2);

        // Then: 잔액 롤백 확인
        Long balanceAfterFailed = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterFailed).isEqualTo(initialBalance);
    }
}
