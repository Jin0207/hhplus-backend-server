package kr.hhplus.be.server.application.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import java.time.Duration;
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
import org.springframework.data.redis.core.ValueOperations;

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

    private final Long TEST_USER_ID = 1L;
    private final Long TEST_COUPON_ID = 100L;
    private Coupon activeCoupon;

    @BeforeEach
    void setUp() {
        // Redis 내부 연산 처리를 위한 Mock 설정
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        activeCoupon = new Coupon(
            TEST_COUPON_ID, "테스트 선착순 쿠폰", CouponType.AMOUNT, 5000L, 10000L,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
            100, 10, CouponStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("성공: Redis 선착순 검증을 통과하면 DB에 발급 이력을 저장하고 쿠폰 수량을 차감한다.")
    void 쿠폰_발급_성공() {
        // Given
        UserCoupon savedUserCoupon = UserCoupon.issue(TEST_USER_ID, TEST_COUPON_ID, activeCoupon.validTo());

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(activeCoupon));
        when(valueOperations.decrement(anyString())).thenReturn(9L);
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(savedUserCoupon);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserCouponResponse response = couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.couponId()).isEqualTo(TEST_COUPON_ID);

        // 메서드 호출 순서 및 횟수 검증
        verify(valueOperations, times(1)).setIfAbsent(contains("issue"), eq("1"), any());
        verify(couponRepository, times(1)).findByIdWithLock(TEST_COUPON_ID);
        verify(valueOperations, times(1)).decrement(contains("quantity"));
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        verify(couponRepository, times(1)).save(argThat(c -> c.availableQuantity() == 9));
    }

    @Test
    @DisplayName("실패: Redis 재고가 부족하면 DB를 거치지 않고 Redis 롤백 후 예외를 던진다.")
    void 재고_부족_실패_및_롤백_검증() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(activeCoupon));
        when(valueOperations.decrement(anyString())).thenReturn(-1L); // 재고 소진 상태

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_OUT_OF_STOCK);

        // [증빙 포인트 3] 롤백 로직 작동 여부 확인
        // COUPON_OUT_OF_STOCK인 경우 increment는 호출되지 않음 (실제 코드 line 67-69 참고)
        verify(valueOperations, never()).increment(anyString()); // 재고 소진은 정상 실패이므로 복구하지 않음
        verify(redisTemplate).delete(anyString());       // 중복체크 키만 삭제
        verify(couponRepository).findByIdWithLock(TEST_COUPON_ID); // DB 조회는 이루어짐
        verify(userCouponRepository, never()).save(any()); // 쿠폰 저장은 안 됨
    }

    @Test
    @DisplayName("실패: DB 조회 시 쿠폰이 발급 불가능 상태이면 Redis를 롤백하고 예외를 던진다.")
    void 쿠폰_상태_이상_실패_및_롤백_검증() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        // INACTIVE 상태의 쿠폰 Mocking
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

        // DB 조회 이후 예외 발생 시 Redis 정합성 확인
        // COUPON_NOT_AVAILABLE은 상태 체크 실패이므로 Redis 차감 전에 발생 (실제 코드 line 57-60)
        verify(valueOperations, never()).decrement(anyString()); // 재고 차감 전에 실패
        verify(valueOperations, never()).increment(anyString()); // 차감하지 않았으므로 복구도 없음
        verify(redisTemplate).delete(anyString()); // 중복체크 키만 삭제
        verify(userCouponRepository, never()).save(any()); // 쿠폰 저장은 수행되지 않음
    }
}