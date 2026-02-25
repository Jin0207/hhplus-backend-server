package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import kr.hhplus.be.server.infrastructure.external.DataPlatformClient;
import kr.hhplus.be.server.infrastructure.external.DataPlatformClient.OrderEventPayload;

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
    private DataPlatformClient dataPlatformClient;

    @Test
    @DisplayName("주문 완료 시 데이터 플랫폼 호출 확인")
    void order_completion_calls_data_platform_test() {
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

        // when: 주문 실행
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

        // [핵심 검증] 데이터 플랫폼 호출이 1번 발생했는지 확인
        // @TransactionalEventListener(phase = AFTER_COMMIT)에 의해
        // 트랜잭션 커밋 후에 호출됨
        verify(dataPlatformClient, times(1)).sendOrderEvent(any(OrderEventPayload.class));
    }

    @Test
    @DisplayName("[좋은 사례] 데이터 플랫폼 호출 실패해도 주문 트랜잭션은 롤백되지 않음")
    void data_platform_failure_does_not_rollback_transaction_test() {
        // given
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("platform_fail_" + timestamp + "@example.com", "password123"));
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

        // [핵심] 데이터 플랫폼 호출을 실패하도록 Mock 설정
        doThrow(new RuntimeException("데이터 플랫폼 호출 실패"))
            .when(dataPlatformClient)
            .sendOrderEvent(any(OrderEventPayload.class));

        String idempotencyKey = UUID.randomUUID().toString();
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 2)),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        Long initialBalance = pointService.getPointBalance(testUser.id());

        // when: 주문 실행 (외부 API 실패에도 불구하고 성공해야 함)
        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // then 1: [좋은 사례] 주문은 정상적으로 완료됨
        // 외부 API 호출 실패가 도메인 트랜잭션에 영향을 주지 않음
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

        // then 5: 데이터 플랫폼 호출 시도는 되었음
        verify(dataPlatformClient, times(1)).sendOrderEvent(any(OrderEventPayload.class));
    }

    @Test
    @DisplayName("다중 상품 주문 시 데이터 플랫폼에 모든 상품 정보 전송 확인")
    void multiple_products_order_sends_all_items_to_data_platform_test() {
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

        // then 2: 데이터 플랫폼 호출 확인
        verify(dataPlatformClient, times(1)).sendOrderEvent(any(OrderEventPayload.class));
    }
}
