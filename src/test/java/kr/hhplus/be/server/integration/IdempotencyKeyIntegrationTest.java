package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

/**
 * Idempotency Key 중복 요청 테스트
 * 동일한 idempotencyKey로 여러 번 요청 시 한 번만 처리되는지 확인
 */
class IdempotencyKeyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        // Redis만 정리 (데이터는 각 테스트가 고유한 이메일 사용하므로 격리됨)
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // 정리 중 에러는 무시
        }
    }

    @Test
    @DisplayName("✅ 동일한 idempotencyKey로 중복 요청 시 예외 발생")
    void duplicateRequestWithSameIdempotencyKeyThrowsException() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("idempotency_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 1000000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(null, "테스트 상품", 100000L, 1000,
                    ProductCategory.TOP, ProductStatus.ON_SALE,
                    0, java.time.LocalDateTime.now(), null)
        );

        String idempotencyKey = UUID.randomUUID().toString();

        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null, 0L, "POINT", idempotencyKey
        );

        // When: 첫 번째 요청 성공
        OrderResponse firstResponse = orderFacade.completeOrder(testUser.id(), orderRequest);
        assertThat(firstResponse).isNotNull();
        assertThat(firstResponse.orderId()).isNotNull();

        // When & Then: 동일한 idempotencyKey로 두 번째 요청 시 예외 발생
        BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> orderFacade.completeOrder(testUser.id(), orderRequest)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("✅ 다른 idempotencyKey로 요청 시 정상 처리")
    void differentIdempotencyKeysAllowMultipleOrders() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("different_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 1000000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(null, "테스트 상품", 100000L, 1000,
                    ProductCategory.TOP, ProductStatus.ON_SALE,
                    0, java.time.LocalDateTime.now(), null)
        );

        String idempotencyKey1 = UUID.randomUUID().toString();
        String idempotencyKey2 = UUID.randomUUID().toString();

        OrderCreateRequest orderRequest1 = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null, 0L, "POINT", idempotencyKey1
        );

        OrderCreateRequest orderRequest2 = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null, 0L, "POINT", idempotencyKey2
        );

        // When: 두 번 주문 (서로 다른 idempotencyKey)
        OrderResponse firstResponse = orderFacade.completeOrder(testUser.id(), orderRequest1);
        OrderResponse secondResponse = orderFacade.completeOrder(testUser.id(), orderRequest2);

        // Then: 두 주문 모두 성공
        assertThat(firstResponse.orderId()).isNotNull();
        assertThat(secondResponse.orderId()).isNotNull();
        assertThat(firstResponse.orderId()).isNotEqualTo(secondResponse.orderId());

        // Then: Payment도 2개 생성됨
        List<Payment> payments = paymentRepository.findByUserId(testUser.id());
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::idempotencyKey)
            .containsExactlyInAnyOrder(idempotencyKey1, idempotencyKey2);
    }

    @Test
    @DisplayName("✅ 동시에 동일한 idempotencyKey로 요청 시 한 번만 성공 (동시성 테스트)")
    void concurrentRequestsWithSameIdempotencyKeyOnlyOneSucceeds() throws InterruptedException {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("concurrent_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 1000000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(null, "테스트 상품", 100000L, 1000,
                    ProductCategory.TOP, ProductStatus.ON_SALE,
                    0, java.time.LocalDateTime.now(), null)
        );

        String idempotencyKey = UUID.randomUUID().toString();
        int threadCount = 10;

        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null, 0L, "POINT", idempotencyKey
        );

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: 동시에 10번 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderFacade.completeOrder(testUser.id(), orderRequest);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    // 분산락 실패, 이미 처리됨, 중복 요청 모두 실패로 카운트
                    if (e.getErrorCode() == ErrorCode.PAYMENT_ALREADY_PROCESSED ||
                        e.getErrorCode() == ErrorCode.DUPLICATE_PAYMENT_REQUEST ||
                        e.getErrorCode() == ErrorCode.LOCK_ACQUISITION_FAILED) {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1번만 성공, 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);

        // Then: Payment도 1개만 생성됨
        Optional<Payment> payment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(payment).isPresent();
    }

    @Test
    @DisplayName("✅ 실패한 결제의 idempotencyKey는 재시도 가능 (포인트 충전 후)")
    void failedPaymentIdempotencyKeyCanBeRetried() {
        // Given: 포인트 부족 상황 (실패하도록 설정, 타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User poorUser = userRepository.save(User.create("poor_" + timestamp + "@example.com", "password123"));
        pointService.chargePoint(poorUser.id(), 1000L, "적은 포인트");

        Product testProduct = productRepository.save(
            new Product(null, "테스트 상품", 100000L, 1000,
                    ProductCategory.TOP, ProductStatus.ON_SALE,
                    0, java.time.LocalDateTime.now(), null)
        );

        String idempotencyKey = UUID.randomUUID().toString();

        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)), // 100,000원
            null, 0L, "POINT", idempotencyKey
        );

        // When: 첫 번째 요청 실패 (포인트 부족)
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> orderFacade.completeOrder(poorUser.id(), orderRequest)
        );

        // When: 포인트 충전 후 동일한 idempotencyKey로 재시도
        pointService.chargePoint(poorUser.id(), 200000L, "추가 충전");

        // Then: 이전에 실패한 idempotencyKey로 재시도 성공
        // 실패한 결제는 롤백되어 idempotencyKey가 저장되지 않으므로 재사용 가능
        OrderResponse response = orderFacade.completeOrder(poorUser.id(), orderRequest);
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isNotNull();

        // Then: 성공 후 동일 idempotencyKey로 재시도 시 예외 발생
        BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> orderFacade.completeOrder(poorUser.id(), orderRequest)
        );

        assertThat(exception.getErrorCode()).isIn(
            ErrorCode.DUPLICATE_PAYMENT_REQUEST,
            ErrorCode.PAYMENT_ALREADY_PROCESSED,
            ErrorCode.LOCK_ACQUISITION_FAILED
        );
    }

    @Test
    @DisplayName("✅ idempotencyKey가 고유하면 같은 사용자가 여러 주문 가능")
    void sameUserCanMakeMultipleOrdersWithDifferentIdempotencyKeys() {
        // Given: 테스트 데이터 생성
        User testUser = userRepository.save(User.create("multiple@example.com", "password123"));
        pointService.chargePoint(testUser.id(), 1000000L, "테스트 충전");

        Product testProduct = productRepository.save(
            new Product(null, "테스트 상품", 100000L, 1000,
                    ProductCategory.TOP, ProductStatus.ON_SALE,
                    0, java.time.LocalDateTime.now(), null)
        );

        int orderCount = 5;

        // When: 5번 주문 (각각 다른 idempotencyKey)
        for (int i = 0; i < orderCount; i++) {
            OrderCreateRequest orderRequest = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
                null, 0L, "POINT",
                UUID.randomUUID().toString() // 매번 새로운 idempotencyKey
            );

            OrderResponse response = orderFacade.completeOrder(testUser.id(), orderRequest);
            assertThat(response.orderId()).isNotNull();
        }

        // Then: Payment가 5개 생성됨
        List<Payment> payments = paymentRepository.findByUserId(testUser.id());
        assertThat(payments).hasSize(orderCount);

        // Then: 모든 idempotencyKey가 고유함
        long uniqueKeyCount = payments.stream()
            .map(Payment::idempotencyKey)
            .distinct()
            .count();
        assertThat(uniqueKeyCount).isEqualTo(orderCount);
    }
}
