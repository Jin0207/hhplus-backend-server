package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 분산 락 통합 테스트
 * Redis Testcontainer를 사용하여 분산 락이 정상 동작하는지 검증
 */
class DistributedLockIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("성공: Redis 기반 분산 락이 동시성을 제어한다")
    void Redis_분산_락_동시성_제어() throws InterruptedException {
        // Given
        String lockKey = "lock:test:concurrent";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockAcquiredCount = new AtomicInteger(0);

        // When: 10개의 스레드가 동시에 락을 획득 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 락 획득 시도 (스핀락 방식)
                    String lockValue = Thread.currentThread().getName();
                    long startTime = System.currentTimeMillis();
                    long waitTime = 5000L;
                    boolean acquired = false;

                    while (System.currentTimeMillis() - startTime < waitTime) {
                        Boolean success = redisTemplate.opsForValue()
                            .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));

                        if (Boolean.TRUE.equals(success)) {
                            acquired = true;
                            lockAcquiredCount.incrementAndGet();
                            break;
                        }

                        Thread.sleep(50);
                    }

                    if (acquired) {
                        try {
                            // 락 내에서 짧은 작업 수행
                            Thread.sleep(100);
                            successCount.incrementAndGet();
                        } finally {
                            // 락 해제
                            String currentValue = redisTemplate.opsForValue().get(lockKey);
                            if (lockValue.equals(currentValue)) {
                                redisTemplate.delete(lockKey);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lock execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then: 모든 요청이 순차적으로 처리됨 (락이 순차적으로 획득됨)
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(lockAcquiredCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("성공: 락 해제 후 다른 스레드가 락을 획득할 수 있다")
    void 락_해제_후_재획득() {
        // Given
        String lockKey = "lock:test:reacquire";
        String firstValue = "first-thread";
        String secondValue = "second-thread";

        // When: 첫 번째 스레드가 락 획득
        Boolean firstAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, firstValue, Duration.ofSeconds(5));
        assertThat(firstAcquired).isTrue();

        // 첫 번째 스레드가 락 해제
        redisTemplate.delete(lockKey);

        // Then: 두 번째 스레드가 락 획득 가능
        Boolean secondAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, secondValue, Duration.ofSeconds(5));
        assertThat(secondAcquired).isTrue();

        // Cleanup
        redisTemplate.delete(lockKey);
    }

    @Test
    @DisplayName("성공: 락 TTL이 만료되면 자동으로 락이 해제된다")
    void 락_TTL_만료() throws InterruptedException {
        // Given
        String lockKey = "lock:test:ttl";
        String lockValue = "test-thread";

        // When: 1초 TTL로 락 획득
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(1));
        assertThat(acquired).isTrue();

        // 2초 대기 (TTL 만료)
        Thread.sleep(2000);

        // Then: TTL 만료 후 다시 락 획득 가능
        Boolean reacquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));
        assertThat(reacquired).isTrue();

        // Cleanup
        redisTemplate.delete(lockKey);
    }
}
