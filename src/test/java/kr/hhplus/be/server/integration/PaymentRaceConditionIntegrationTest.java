package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
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
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;

import java.util.List;
import java.util.Set;

public class PaymentRaceConditionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화 (기존 멱등성 키 + 분산락 키 모두 정리)
        Set<String> paymentKeys = redisTemplate.keys("payment:idempotency:*");
        if (paymentKeys != null && !paymentKeys.isEmpty()) {
            redisTemplate.delete(paymentKeys);
        }
        Set<String> lockKeys = redisTemplate.keys("lock:*");
        if (lockKeys != null && !lockKeys.isEmpty()) {
            redisTemplate.delete(lockKeys);
        }
    }

    @AfterEach
    void tearDown() {
        // Redis 정리 (기존 멱등성 키 + 분산락 키 모두 정리)
        Set<String> paymentKeys = redisTemplate.keys("payment:idempotency:*");
        if (paymentKeys != null && !paymentKeys.isEmpty()) {
            redisTemplate.delete(paymentKeys);
        }
        Set<String> lockKeys = redisTemplate.keys("lock:*");
        if (lockKeys != null && !lockKeys.isEmpty()) {
            redisTemplate.delete(lockKeys);
        }
    }

    @Test
    @DisplayName("성공: 동일 사용자가 동일 멱등성 키로 50번 동시 결제 요청 - 1번만 성공")
    void sameUserDuplicatePaymentRequest_OnlyOneSucceeds() throws InterruptedException {
        // Given: 충분한 포인트를 가진 사용자 생성
        long timestamp = System.currentTimeMillis();
        User user = userRepository.save(
            User.create("payment_test_user_" + timestamp + "@example.com", "password123")
                .chargePoint(1000000L) // 100만 포인트 충전
        );

        // Given: 재고가 충분한 상품 생성
        Product product = productRepository.save(
            new Product(
                null,
                "결제 테스트 상품",
                10000L,
                1000,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                LocalDateTime.now(),
                null
            )
        );

        // Given: 동일한 멱등성 키
        String idempotencyKey = "test_payment_key_" + timestamp;

        // Given: 주문 요청 데이터
        OrderCreateRequest request = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(product.id(), 1)),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        // When: 50개 스레드가 동시에 동일한 주문 요청
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateRequestCount = new AtomicInteger(0);
        AtomicInteger alreadyProcessedCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderFacade.completeOrder(user.id(), request);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    String errorCode = e.getErrorCode().getCode();
                    if ("E303".equals(errorCode)) {
                        // DUPLICATE_PAYMENT_REQUEST
                        duplicateRequestCount.incrementAndGet();
                    } else if ("E302".equals(errorCode)) {
                        // PAYMENT_ALREADY_PROCESSED
                        alreadyProcessedCount.incrementAndGet();
                    } else if ("E600".equals(errorCode)) {
                        // LOCK_ACQUISITION_FAILED - 분산락 획득 실패도 중복 요청으로 처리
                        duplicateRequestCount.incrementAndGet();
                    } else {
                        otherErrorCount.incrementAndGet();
                        System.err.println("Unexpected error: " + errorCode + " - " + e.getMessage());
                    }
                } catch (Exception e) {
                    otherErrorCount.incrementAndGet();
                    System.err.println("Unexpected exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 정확히 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateRequestCount.get() + alreadyProcessedCount.get()).isEqualTo(threadCount - 1);

        // Then: DB에 주문 1개만 생성됨
        List<Order> orders = orderRepository.findByUserId(user.id());
        assertThat(orders).hasSize(1);

        // Then: DB에 결제 1개만 생성됨
        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        assertThat(payment).isNotNull();

        // 결과 로깅
        System.out.println("===========================================");
        System.out.println("중복 결제 방지 테스트 결과");
        System.out.println("===========================================");
        System.out.println("총 요청 수: " + threadCount + "회");
        System.out.println("성공: " + successCount.get() + "회");
        System.out.println("중복 요청 차단: " + duplicateRequestCount.get() + "회");
        System.out.println("이미 처리됨: " + alreadyProcessedCount.get() + "회");
        System.out.println("기타 에러: " + otherErrorCount.get() + "회");
        System.out.println("실행 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");
        System.out.println("TPS: " + (threadCount * 1000.0 / duration) + " 요청/초");
        System.out.println("===========================================");
    }

    @Test
    @DisplayName("성공: 다른 멱등성 키로 동시 결제 요청 - 모두 성공")
    void differentIdempotencyKeys_AllSucceed() throws InterruptedException {
        // Given: 충분한 포인트를 가진 사용자
        long timestamp = System.currentTimeMillis();
        User user = userRepository.save(
            User.create("multi_payment_user_" + timestamp + "@example.com", "password123")
                .chargePoint(1000000L) // 100만 포인트 충전 (최대 한도)
        );

        // Given: 재고가 충분한 상품
        Product product = productRepository.save(
            new Product(
                null,
                "다중 결제 테스트 상품",
                10000L,
                1000,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                LocalDateTime.now(),
                null
            )
        );

        // When: 10개의 다른 멱등성 키로 동시 결제
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    String uniqueKey = "multi_payment_key_" + timestamp + "_" + index;
                    OrderCreateRequest request = new OrderCreateRequest(
                        List.of(new OrderCreateRequest.OrderItem(product.id(), 1)),
                        null,
                        0L,
                        "POINT",
                        uniqueKey
                    );
                    orderFacade.completeOrder(user.id(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Payment failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 모두 성공
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isEqualTo(0);

        // Then: DB에 주문 10개 생성됨
        List<Order> orders = orderRepository.findByUserId(user.id());
        assertThat(orders.size()).isGreaterThanOrEqualTo(threadCount);

        // 결과 로깅
        System.out.println("===========================================");
        System.out.println("다중 결제 동시 처리 테스트 결과");
        System.out.println("===========================================");
        System.out.println("총 요청 수: " + threadCount + "회");
        System.out.println("성공: " + successCount.get() + "회");
        System.out.println("실패: " + failureCount.get() + "회");
        System.out.println("실행 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");
        System.out.println("TPS: " + (threadCount * 1000.0 / duration) + " 요청/초");
        System.out.println("===========================================");
    }

    @Test
    @DisplayName("동시성 테스트 - 100명이 동시에 각각 중복 결제 시도")
    void extremeConcurrency_100UsersWithDuplicateRequests() throws InterruptedException {
        // Given: 100명의 사용자와 상품 생성
        long timestamp = System.currentTimeMillis();
        int userCount = 100;
        User[] users = new User[userCount];

        for (int i = 0; i < userCount; i++) {
            users[i] = userRepository.save(
                User.create("extreme_user_" + i + "_" + timestamp + "@example.com", "password123")
                    .chargePoint(1000000L)
            );
        }

        Product product = productRepository.save(
            new Product(
                null,
                "극한 테스트 상품",
                10000L,
                10000,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                LocalDateTime.now(),
                null
            )
        );

        // When: 각 사용자당 5번씩 동시 요청 (총 500개 요청)
        int requestsPerUser = 5;
        int totalRequests = userCount * requestsPerUser;
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalDuplicate = new AtomicInteger(0);
        AtomicInteger totalOtherError = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < userCount; i++) {
            final User user = users[i];
            final String idempotencyKey = "extreme_key_" + timestamp + "_user_" + i;

            for (int j = 0; j < requestsPerUser; j++) {
                executorService.submit(() -> {
                    try {
                        OrderCreateRequest request = new OrderCreateRequest(
                            List.of(new OrderCreateRequest.OrderItem(product.id(), 1)),
                            null,
                            0L,
                            "POINT",
                            idempotencyKey
                        );
                        orderFacade.completeOrder(user.id(), request);
                        totalSuccess.incrementAndGet();
                    } catch (BusinessException e) {
                        String errorCode = e.getErrorCode().getCode();
                        // E303: DUPLICATE_PAYMENT_REQUEST, E302: PAYMENT_ALREADY_PROCESSED, E600: LOCK_ACQUISITION_FAILED
                        if ("E303".equals(errorCode) || "E302".equals(errorCode) || "E600".equals(errorCode)) {
                            totalDuplicate.incrementAndGet();
                        } else {
                            totalOtherError.incrementAndGet();
                        }
                    } catch (Exception e) {
                        totalOtherError.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 각 사용자당 정확히 1번씩만 성공 (총 100번)
        assertThat(totalSuccess.get()).isEqualTo(userCount);
        assertThat(totalDuplicate.get()).isEqualTo(totalRequests - userCount);

        // Then: 각 사용자의 주문이 정확히 1개씩 생성됨
        for (User user : users) {
            List<Order> userOrders = orderRepository.findByUserId(user.id());
            assertThat(userOrders).hasSize(1);
        }

        // 결과 로깅
        System.out.println("===========================================");
        System.out.println("극한 동시성 테스트 결과");
        System.out.println("===========================================");
        System.out.println("사용자 수: " + userCount + "명");
        System.out.println("사용자당 요청 수: " + requestsPerUser + "회");
        System.out.println("총 요청 수: " + totalRequests + "회");
        System.out.println("성공: " + totalSuccess.get() + "회");
        System.out.println("중복 차단: " + totalDuplicate.get() + "회");
        System.out.println("기타 에러: " + totalOtherError.get() + "회");
        System.out.println("실행 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");
        System.out.println("TPS: " + (totalRequests * 1000.0 / duration) + " 요청/초");
        System.out.println("===========================================");
    }
}
