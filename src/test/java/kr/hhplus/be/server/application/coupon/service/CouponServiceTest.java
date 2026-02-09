package kr.hhplus.be.server.application.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.UserCouponRepository;
import kr.hhplus.be.server.presentation.coupon.dto.response.UserCouponResponse;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService TDD")
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private final Long TEST_USER_ID = 1L;
    private final Long TEST_COUPON_ID = 100L;
    private Coupon activeCoupon;

    @BeforeEach
    void setUp() {
        // Redis 내부 연산 처리를 위한 Mock 설정
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        activeCoupon = new Coupon(
            TEST_COUPON_ID, "테스트 선착순 쿠폰", CouponType.AMOUNT, 5000L, 10000L,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
            100, 10, CouponStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("성공: Set 중복 체크 + Sorted Set 선착순 검증을 통과하면 DB에 발급 이력을 저장하고 쿠폰 수량을 차감한다.")
    void 쿠폰_발급_성공() {
        // Given
        UserCoupon savedUserCoupon = UserCoupon.issue(TEST_USER_ID, TEST_COUPON_ID, activeCoupon.validTo());

        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSetOperations.rank(anyString(), anyString())).thenReturn(0L);
        when(valueOperations.get(anyString())).thenReturn("10");
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(activeCoupon));
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(savedUserCoupon);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserCouponResponse response = couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.couponId()).isEqualTo(TEST_COUPON_ID);

        // Redis 호출 검증: Set(중복체크) → Sorted Set(대기열 등록) → Sorted Set(순위 확인) → DB
        verify(setOperations, times(1)).add(contains("issued"), eq(String.valueOf(TEST_USER_ID)));
        verify(zSetOperations, times(1)).add(contains("request"), eq(String.valueOf(TEST_USER_ID)), anyDouble());
        verify(zSetOperations, times(1)).rank(contains("request"), eq(String.valueOf(TEST_USER_ID)));
        verify(couponRepository, times(1)).findByIdWithLock(TEST_COUPON_ID);
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        verify(couponRepository, times(1)).save(argThat(c -> c.availableQuantity() == 9));
    }

    @Test
    @DisplayName("실패: Sorted Set 순위가 수량 이상이면 DB를 거치지 않고 Redis를 롤백하고 예외를 던진다.")
    void 재고_부족_실패_및_롤백_검증() {
        // Given
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSetOperations.rank(anyString(), anyString())).thenReturn(10L); // rank(10) >= maxQuantity(10) → 탈락
        when(valueOperations.get(anyString())).thenReturn("10");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_OUT_OF_STOCK);

        // 롤백 검증: Set에서 사용자 제거 + Sorted Set에서 대기열 제거
        verify(setOperations).remove(contains("issued"), eq(String.valueOf(TEST_USER_ID)));
        verify(zSetOperations).remove(contains("request"), eq(String.valueOf(TEST_USER_ID)));

        // DB는 호출되지 않음 (Redis 레벨에서 조기 차단)
        verify(couponRepository, never()).findByIdWithLock(anyLong());
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: DB 조회 시 쿠폰이 비활성 상태이면 Redis를 롤백하고 예외를 던진다.")
    void 쿠폰_상태_이상_실패_및_롤백_검증() {
        // Given
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSetOperations.rank(anyString(), anyString())).thenReturn(0L); // 순위 통과
        when(valueOperations.get(anyString())).thenReturn("10");

        Coupon inactiveCoupon = new Coupon(
            TEST_COUPON_ID, "비활성 쿠폰", CouponType.AMOUNT, 5000L, 10000L,
            activeCoupon.validFrom(), activeCoupon.validTo(), 100, 10, CouponStatus.INACTIVE,
            activeCoupon.crtDttm(), activeCoupon.updDttm()
        );
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(inactiveCoupon));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);

        // Redis 롤백 검증: Set + Sorted Set 모두 정리
        verify(setOperations).remove(contains("issued"), eq(String.valueOf(TEST_USER_ID)));
        verify(zSetOperations).remove(contains("request"), eq(String.valueOf(TEST_USER_ID)));

        // DB 조회는 이루어짐 (순위 검증 통과 후 DB 조회)
        verify(couponRepository).findByIdWithLock(TEST_COUPON_ID);
        // 쿠폰 저장은 수행되지 않음
        verify(userCouponRepository, never()).save(any());
    }
}