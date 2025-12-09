package kr.hhplus.be.server.domain.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.redis.core.ValueOperations;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
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
        // RedisTemplate이 ValueOperations를 반환하도록 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LocalDateTime now = LocalDateTime.now();
        activeCoupon = new Coupon(
                TEST_COUPON_ID, "테스트 선착순 쿠폰", CouponType.AMOUNT, 5000, 10000,
                now.minusDays(1), now.plusDays(1), 100, 10, CouponStatus.ACTIVE,
                now.minusDays(2), now.minusDays(2)
        );
    }

    @Test
    @DisplayName("성공: 선착순 쿠폰 발급 시 Redis 차감 및 DB 저장에 성공해야 한다.")
    void 쿠폰_발급_성공() {
        // Given
        UserCoupon savedUserCoupon = UserCoupon.issue(TEST_USER_ID, TEST_COUPON_ID, activeCoupon.validTo());
        
        // 1. Redis 중복 발급 체크: 처음 시도이므로 true 반환
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        
        // 2. Redis 재고 차감: 재고가 남아있으므로 차감 후 남은 수량(9) 반환
        when(valueOperations.decrement(anyString())).thenReturn(9L);

        // 3. DB 조회 (LOCK 포함): 유효한 쿠폰 반환
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(activeCoupon));
        
        // 4. DB UserCoupon 저장: 저장된 객체 반환
        when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(savedUserCoupon);
        
        // 5. DB Coupon 업데이트: save 호출 시 업데이트된 Coupon 반환
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0)); 

        // When
        UserCouponResponse response = couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID);

        // Then
        assertThat(response.userId()).isEqualTo(TEST_USER_ID);
        assertThat(response.couponId()).isEqualTo(TEST_COUPON_ID);
        assertThat(response.status()).isEqualTo(UserCouponStatus.AVAILABLE);

        // Verify
        verify(valueOperations, times(1)).setIfAbsent(anyString(), eq("1"), any());
        verify(valueOperations, times(1)).decrement(anyString());
        verify(couponRepository, times(1)).findByIdWithLock(TEST_COUPON_ID);
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        verify(couponRepository, times(1)).save(argThat(coupon -> coupon.availableQuantity() == 9));
        // 롤백 관련 메서드는 호출되지 않아야 함
        verify(valueOperations, never()).increment(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    // --------------------------------------------------------------------------------
    // 실패 케이스
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("실패: 이미 발급된 쿠폰을 재발급 시도 시 COUPON_ALREADY_ISSUED 예외를 던져야 한다.")
    void 중복_발급_실패() {
        // Given
        // 1. Redis 중복 발급 체크: 이미 발급되어 false 반환 (실패)
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);

        // Verify: Redis 중복 방지 체크만 호출되고, 후속 로직은 호출되지 않아야 함
        verify(valueOperations, times(1)).setIfAbsent(anyString(), anyString(), any());
        verify(valueOperations, never()).decrement(anyString());
        verify(couponRepository, never()).findByIdWithLock(anyLong());
    }

    @Test
    @DisplayName("실패: Redis 재고가 부족하면 COUPON_OUT_OF_STOCK 예외를 던지고 Redis를 롤백해야 한다.")
    void 재고_부족_실패() {
        // Given
        // 1. Redis 중복 발급 체크: 성공 (true)
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        
        // 2. Redis 재고 차감: 재고 부족으로 -1 반환 (실패)
        when(valueOperations.decrement(anyString())).thenReturn(-1L);
        
        // Redis 롤백 시 재고 복구 (increment) Mocking
        when(valueOperations.increment(anyString())).thenReturn(0L); 

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_OUT_OF_STOCK);

        // Verify: Redis 롤백 로직 검증 (재고 복구 및 발급 키 삭제)
        verify(valueOperations, times(1)).decrement(anyString());
        verify(valueOperations, times(1)).increment(argThat(s -> s.contains("coupon:quantity:100")));
        verify(redisTemplate, times(1)).delete((String)argThat((String s) -> s.contains("coupon:issue:100:user:1")));
        // DB 로직은 호출되지 않아야 함
        verify(couponRepository, never()).findByIdWithLock(anyLong());
    }


    @Test
    @DisplayName("실패: DB 조회 후 쿠폰이 ACTIVE 상태가 아니면 COUPON_NOT_AVAILABLE 예외를 던지고 Redis를 롤백해야 한다.")
    void 쿠폰_비활성화_실패() {
        // Given
        // 1. Redis 중복 발급 체크: 성공 (true)
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        
        // 2. Redis 재고 차감: 성공 (9L)
        when(valueOperations.decrement(anyString())).thenReturn(9L);

        // 3. DB 조회: 유효하지 않은 쿠폰 (INACTIVE 상태) 반환
        Coupon inactiveCoupon = new Coupon(
                TEST_COUPON_ID, "비활성 쿠폰", CouponType.AMOUNT, 5000, 10000,
                activeCoupon.validFrom(), activeCoupon.validTo(), 100, 10, CouponStatus.INACTIVE, 
                activeCoupon.crtDttm(), activeCoupon.updDttm()
        );

        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(inactiveCoupon));
        
        // Redis 롤백 시 재고 복구 (increment) Mocking
        when(valueOperations.increment(anyString())).thenReturn(10L); 

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponService.issueCoupon(TEST_USER_ID, TEST_COUPON_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);

        // Verify: Redis 롤백 로직 검증
        verify(valueOperations, times(1)).decrement(anyString());
        verify(couponRepository, times(1)).findByIdWithLock(TEST_COUPON_ID);
        // DB 저장 로직은 호출되지 않아야 함
        verify(userCouponRepository, never()).save(any());
        // Redis 롤백 (재고 복구 및 발급 키 삭제) 확인
        verify(valueOperations, times(1)).increment(argThat(s -> s.contains("coupon:quantity:100")));
        verify(redisTemplate, times(1)).delete(
            (String) argThat((String s) -> s.contains("coupon:issue:100:user:1")) 
        );
    }
}