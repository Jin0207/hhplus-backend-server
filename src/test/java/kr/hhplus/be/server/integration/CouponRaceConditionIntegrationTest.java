package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.config.TestConfig;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.UserCouponRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;

/**
 * 쿠폰 발급 경쟁 조건(Race Condition) 테스트
 * 선착순 쿠폰 발급 시 동시 요청에서 한 사용자만 성공하는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class CouponRaceConditionIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("✅ 선착순 쿠폰 - 동시 요청 시 한 명만 성공")
    void onlyOneUserSucceedsWhenMultipleUsersRequestSameCoupon() throws InterruptedException {
        // Given: 수량 1개인 선착순 쿠폰 생성
        Coupon limitedCoupon = couponRepository.save(
            new Coupon(
                null,
                "선착순 1개 쿠폰",
                CouponType.AMOUNT,
                10000L,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1, // 총 수량 1개
                1, // 남은 수량 1개
                CouponStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
        );

        // Redis에 쿠폰 수량 초기화
        String quantityKey = COUPON_QUANTITY_KEY + limitedCoupon.id();
        redisTemplate.opsForValue().set(quantityKey, "1");

        // Given: 10명의 사용자 생성
        int userCount = 10;
        User[] users = new User[userCount];
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            users[i] = userRepository.save(
                User.create("user" + i + "_" + timestamp + "@example.com", "password123")
            );
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: 10명의 사용자가 동시에 쿠폰 발급 요청
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(users[userIndex].id(), limitedCoupon.id());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1명만 성공, 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(userCount - 1);

        // Then: DB에 UserCoupon이 1개만 생성됨
        long userCouponCount = userCouponRepository.findAllByCouponId(limitedCoupon.id()).size();
        assertThat(userCouponCount).isEqualTo(1);

        // Then: 쿠폰 수량 확인
        Coupon updatedCoupon = couponRepository.findById(limitedCoupon.id()).orElseThrow();
        assertThat(updatedCoupon.availableQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("✅ 선착순 쿠폰 - 100개 수량에 200명 요청 시 100명만 성공")
    void limitedQuantityCouponIssuedCorrectly() throws InterruptedException {
        // Given: 수량 100개인 선착순 쿠폰
        int couponQuantity = 100;
        Coupon limitedCoupon = couponRepository.save(
            new Coupon(
                null,
                "선착순 100개 쿠폰",
                CouponType.AMOUNT,
                5000L,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                couponQuantity,
                couponQuantity,
                CouponStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
        );

        // Redis에 쿠폰 수량 초기화
        String quantityKey = COUPON_QUANTITY_KEY + limitedCoupon.id();
        redisTemplate.opsForValue().set(quantityKey, String.valueOf(couponQuantity));

        // Given: 200명의 사용자 생성
        int userCount = 200;
        User[] users = new User[userCount];
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            users[i] = userRepository.save(
                User.create("user" + i + "_" + timestamp + "@example.com", "password123")
            );
        }

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: 200명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(users[userIndex].id(), limitedCoupon.id());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 100명만 성공
        assertThat(successCount.get()).isEqualTo(couponQuantity);
        assertThat(failureCount.get()).isEqualTo(userCount - couponQuantity);

        // Then: DB에 UserCoupon이 100개만 생성됨
        long userCouponCount = userCouponRepository.findAllByCouponId(limitedCoupon.id()).size();
        assertThat(userCouponCount).isEqualTo(couponQuantity);

        // Then: 쿠폰 수량 확인
        Coupon updatedCoupon = couponRepository.findById(limitedCoupon.id()).orElseThrow();
        assertThat(updatedCoupon.availableQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("✅ 동일 사용자가 동일 쿠폰 중복 발급 시도 시 1번만 성공")
    void sameUserCannotIssueSameCouponTwice() throws InterruptedException {
        // Given: 수량 충분한 쿠폰
        Coupon coupon = couponRepository.save(
            new Coupon(
                null,
                "중복 방지 테스트 쿠폰",
                CouponType.AMOUNT,
                10000L,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                100,
                100,
                CouponStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
        );

        // Redis에 쿠폰 수량 초기화
        String quantityKey = COUPON_QUANTITY_KEY + coupon.id();
        redisTemplate.opsForValue().set(quantityKey, "100");

        // Given: 단일 사용자
        long timestamp = System.currentTimeMillis();
        User user = userRepository.save(User.create("duplicate_" + timestamp + "@example.com", "password123"));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: 동일 사용자가 동시에 10번 발급 요청
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(user.id(), coupon.id());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);

        // Then: DB에 UserCoupon이 1개만 생성됨
        boolean hasAvailableCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndStatus(user.id(), coupon.id(), UserCouponStatus.AVAILABLE)
            .isPresent();
        assertThat(hasAvailableCoupon).isTrue();
    }

    @Test
    @DisplayName("극한 동시성 테스트 - 1000명이 50개 쿠폰 경쟁")
    void extremeConcurrencyTest_1000Users_50Coupons() throws InterruptedException {
        // Given: 수량 50개인 선착순 쿠폰
        int couponQuantity = 50;
        Coupon limitedCoupon = couponRepository.save(
            new Coupon(
                null,
                "극한 동시성 테스트 쿠폰",
                CouponType.AMOUNT,
                10000L,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                couponQuantity,
                couponQuantity,
                CouponStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
        );

        // Redis에 쿠폰 수량 초기화
        String quantityKey = COUPON_QUANTITY_KEY + limitedCoupon.id();
        redisTemplate.opsForValue().set(quantityKey, String.valueOf(couponQuantity));

        // Given: 1000명의 사용자 생성
        int userCount = 1000;
        User[] users = new User[userCount];
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            users[i] = userRepository.save(
                User.create("extreme_user" + i + "_" + timestamp + "@example.com", "password123")
            );
        }

        // When: 1000명이 동시에 50개 쿠폰 발급 요청 (극한 경쟁)
        // ThreadPool을 200개로 설정하여 더 높은 동시성 보장
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(users[userIndex].id(), limitedCoupon.id());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 정확히 50명만 성공
        assertThat(successCount.get()).isEqualTo(couponQuantity);
        assertThat(failureCount.get()).isEqualTo(userCount - couponQuantity);

        // Then: DB에 UserCoupon이 50개만 생성됨
        long userCouponCount = userCouponRepository.findAllByCouponId(limitedCoupon.id()).size();
        assertThat(userCouponCount).isEqualTo(couponQuantity);

        // Then: 쿠폰 수량 확인
        Coupon updatedCoupon = couponRepository.findById(limitedCoupon.id()).orElseThrow();
        assertThat(updatedCoupon.availableQuantity()).isEqualTo(0);

        // 성능 로그 출력
        System.out.println("===========================================");
        System.out.println("극한 동시성 테스트 결과");
        System.out.println("===========================================");
        System.out.println("총 요청 수: " + userCount + "명");
        System.out.println("쿠폰 수량: " + couponQuantity + "개");
        System.out.println("성공: " + successCount.get() + "명");
        System.out.println("실패: " + failureCount.get() + "명");
        System.out.println("실행 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");
        System.out.println("TPS: " + (userCount * 1000.0 / duration) + " 요청/초");
        System.out.println("===========================================");
    }

    @Test
    @DisplayName("동시성 테스트 - 500명이 10개 쿠폰 경쟁 (경쟁률 50:1)")
    void extremeConcurrencyTest_500Users_10Coupons() throws InterruptedException {
        // Given: 수량 10개인 선착순 쿠폰 (경쟁률 50배)
        int couponQuantity = 10;
        Coupon limitedCoupon = couponRepository.save(
            new Coupon(
                null,
                "초고경쟁 쿠폰",
                CouponType.AMOUNT,
                15000L,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                couponQuantity,
                couponQuantity,
                CouponStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
        );

        // Redis에 쿠폰 수량 초기화
        String quantityKey = COUPON_QUANTITY_KEY + limitedCoupon.id();
        redisTemplate.opsForValue().set(quantityKey, String.valueOf(couponQuantity));

        // Given: 500명의 사용자 생성
        int userCount = 500;
        User[] users = new User[userCount];
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            users[i] = userRepository.save(
                User.create("race_user" + i + "_" + timestamp + "@example.com", "password123")
            );
        }

        // When: 500명이 동시에 10개 쿠폰 발급 요청 (경쟁률 50:1)
        // 최대 동시성을 위해 ThreadPool 크기를 크게 설정
        ExecutorService executorService = Executors.newFixedThreadPool(250);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(users[userIndex].id(), limitedCoupon.id());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 정확히 10명만 성공
        assertThat(successCount.get()).isEqualTo(couponQuantity);
        assertThat(failureCount.get()).isEqualTo(userCount - couponQuantity);

        // Then: DB에 UserCoupon이 10개만 생성됨
        long userCouponCount = userCouponRepository.findAllByCouponId(limitedCoupon.id()).size();
        assertThat(userCouponCount).isEqualTo(couponQuantity);

        // Then: 쿠폰 수량 확인
        Coupon updatedCoupon = couponRepository.findById(limitedCoupon.id()).orElseThrow();
        assertThat(updatedCoupon.availableQuantity()).isEqualTo(0);

        // 성능 로그 출력
        System.out.println("===========================================");
        System.out.println("초고경쟁 동시성 테스트 결과 (경쟁률 50:1)");
        System.out.println("===========================================");
        System.out.println("총 요청 수: " + userCount + "명");
        System.out.println("쿠폰 수량: " + couponQuantity + "개");
        System.out.println("경쟁률: " + (userCount / couponQuantity) + ":1");
        System.out.println("성공: " + successCount.get() + "명");
        System.out.println("실패: " + failureCount.get() + "명");
        System.out.println("실행 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");
        System.out.println("TPS: " + (userCount * 1000.0 / duration) + " 요청/초");
        System.out.println("===========================================");
    }

    @Test
    @DisplayName("성공: 쿠폰 사용 후 복구 시 재발급 가능")
    void canReissueCouponAfterUseAndRestore() {
        // Given: 쿠폰 생성
        Coupon coupon = couponRepository.save(
            new Coupon(
                null,
                "재발급 테스트 쿠폰",
                CouponType.AMOUNT,
                10000L,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                10,
                10,
                CouponStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
        );

        // Redis에 쿠폰 수량 초기화
        String quantityKey = COUPON_QUANTITY_KEY + coupon.id();
        redisTemplate.opsForValue().set(quantityKey, "10");

        long timestamp = System.currentTimeMillis();
        User user = userRepository.save(User.create("reissue_" + timestamp + "@example.com", "password123"));

        // When: 쿠폰 발급
        couponService.issueCoupon(user.id(), coupon.id());

        // Then: 발급 성공 확인
        UserCoupon issuedCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndStatus(user.id(), coupon.id(), UserCouponStatus.AVAILABLE)
            .orElseThrow();
        assertThat(issuedCoupon.status()).isEqualTo(UserCouponStatus.AVAILABLE);

        // When: 쿠폰 사용
        couponService.useCoupon(user.id(), coupon.id());

        // Then: 사용 완료 확인
        UserCoupon usedCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndStatus(user.id(), coupon.id(), UserCouponStatus.USED)
            .orElseThrow();
        assertThat(usedCoupon.status()).isEqualTo(UserCouponStatus.USED);

        // When: 쿠폰 복구 (주문 취소)
        couponService.restoreCoupon(user.id(), coupon.id());

        // Then: 복구 확인
        UserCoupon restoredCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndStatus(user.id(), coupon.id(), UserCouponStatus.AVAILABLE)
            .orElseThrow();
        assertThat(restoredCoupon.status()).isEqualTo(UserCouponStatus.AVAILABLE);
    }
}
