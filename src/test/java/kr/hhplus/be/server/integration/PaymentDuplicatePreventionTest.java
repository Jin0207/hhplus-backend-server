package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentDuplicatePreventionTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        Set<String> keys = redisTemplate.keys("payment:idempotency:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @AfterEach
    void tearDown() {
        // Redis 정리
        Set<String> keys = redisTemplate.keys("payment:idempotency:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("동일 사용자가 동일 멱등성 키로 10번 동시 결제 요청 - 1번만 성공")
    void duplicatePaymentRequest_OnlyOneSucceeds() throws InterruptedException {
        // Given: 충분한 포인트를 가진 사용자
        long timestamp = System.currentTimeMillis();
        User user = userRepository.save(
            User.create("test_user_" + timestamp + "@example.com", "password123")
                .chargePoint(100000L)
        );

        // Given: 재고가 충분한 상품
        Product product = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                10000L,
                100,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                LocalDateTime.now(),
                null
            )
        );

        // Given: 동일한 멱등성 키
        String idempotencyKey = "test_key_" + timestamp;

        // Given: 주문 요청 데이터
        OrderCreateRequest request = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(product.id(), 1)),
            null,
            0L,
            "POINT",
            idempotencyKey
        );

        // When: 10개 스레드가 동시에 동일한 주문 요청
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderFacade.completeOrder(user.id(), request);
                    successCount.incrementAndGet();
                    System.out.println("Success");
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                    System.out.println("Failed: " + e.getErrorCode().getCode() + " - " + e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("Error: " + e.getMessage());
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
        System.out.println("===========================================");
        System.out.println("중복 결제 방지 테스트 결과");
        System.out.println("===========================================");
        System.out.println("총 요청 수: " + threadCount + "회");
        System.out.println("성공: " + successCount.get() + "회");
        System.out.println("실패: " + failureCount.get() + "회");
        System.out.println("실행 시간: " + duration + "ms");
        System.out.println("===========================================");

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);

        // Then: DB에 주문 1개만 생성됨
        List<Order> orders = orderRepository.findByUserId(user.id());
        System.out.println("DB 주문 개수: " + orders.size());
        assertThat(orders).hasSize(1);
    }
}
