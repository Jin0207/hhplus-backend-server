package kr.hhplus.be.server.application.coupon.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.UserCouponRepository;
import kr.hhplus.be.server.presentation.coupon.dto.response.UserCouponResponse;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_ISSUED_SET_KEY = "coupon:issued:";
    private static final String COUPON_REQUEST_QUEUE_KEY = "coupon:request:";
    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";

    /**
     * 선착순 쿠폰 발급
     *
     * Redis 자료구조 활용:
     * - Set (coupon:issued:{couponId}): 중복 발급 방지 (SADD 원자적 중복 체크)
     * - Sorted Set (coupon:request:{couponId}): 선착순 대기열 (ZADD + ZRANK로 순위 기반 검증)
     * - String (coupon:quantity:{couponId}): 최대 발급 수량 참조 (읽기 전용)
     */
    @Transactional
    public UserCouponResponse issueCoupon(Long userId, Long couponId) {
        String issuedSetKey = COUPON_ISSUED_SET_KEY + couponId;
        String requestQueueKey = COUPON_REQUEST_QUEUE_KEY + couponId;
        String quantityKey = COUPON_QUANTITY_KEY + couponId;
        String userIdStr = String.valueOf(userId);

        // 1. 중복 발급 방지 (Set: SADD - 원자적 중복 체크)
        Long added = redisTemplate.opsForSet().add(issuedSetKey, userIdStr);
        if (added == null || added == 0) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        try {
            // 2. 선착순 대기열 등록 (Sorted Set: ZADD - score=timestamp로 요청 순서 기록)
            redisTemplate.opsForZSet().add(requestQueueKey, userIdStr, (double) System.currentTimeMillis());

            // 3. 순위 확인으로 선착순 검증 (Sorted Set: ZRANK - 0-based 순위)
            Long rank = redisTemplate.opsForZSet().rank(requestQueueKey, userIdStr);
            String quantityStr = redisTemplate.opsForValue().get(quantityKey);

            if (rank == null || quantityStr == null) {
                throw new BusinessException(ErrorCode.COUPON_ISSUE_FAILED);
            }

            long maxQuantity = Long.parseLong(quantityStr);
            if (rank >= maxQuantity) {
                throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            // 4. 쿠폰 조회 (비관적 락 - 동시성 제어)
            Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            // 유효기간 및 상태 체크
            if (!coupon.isActive()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            // 5. DB 처리 (UserCoupon 발급 및 쿠폰 수량 차감)
            UserCoupon userCoupon = UserCoupon.issue(userId, couponId, coupon.validTo());
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            // DB 쿠폰 수량 차감 (비관적 락으로 동시성 제어)
            Coupon updatedCoupon = coupon.decreaseQuantity();
            couponRepository.save(updatedCoupon);

            log.info("쿠폰 발급 성공: userId={}, couponId={}, rank={}",
                userId, couponId, rank);

            return UserCouponResponse.from(savedUserCoupon, coupon);

        } catch (BusinessException e) {
            rollbackRedis(issuedSetKey, requestQueueKey, userIdStr);
            throw e;
        } catch (Exception e) {
            rollbackRedis(issuedSetKey, requestQueueKey, userIdStr);
            throw new BusinessException(ErrorCode.COUPON_ISSUE_FAILED, e);
        }
    }

    /**
     * Redis 롤백 (Set에서 사용자 제거 + Sorted Set에서 대기열 제거)
     */
    private void rollbackRedis(String issuedSetKey, String requestQueueKey, String userIdStr) {
        try {
            redisTemplate.opsForSet().remove(issuedSetKey, userIdStr);
            redisTemplate.opsForZSet().remove(requestQueueKey, userIdStr);
            log.warn("Redis 롤백 완료: issuedSetKey={}, requestQueueKey={}, userId={}",
                issuedSetKey, requestQueueKey, userIdStr);
        } catch (Exception e) {
            log.error("Redis 롤백 실패: issuedSetKey={}, requestQueueKey={}, userId={}",
                issuedSetKey, requestQueueKey, userIdStr, e);
        }
    }
    /**
     * 보유 쿠폰 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<UserCouponResponse> getUserCoupons(Long userId, Pageable pageable) {
        Page<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId, pageable);

        List<Long> couponIds = userCoupons.stream()
            .map(UserCoupon::couponId)
            .toList();

        Map<Long, Coupon> couponMap = couponRepository.findAllById(couponIds)
            .stream()
            .collect(Collectors.toMap(Coupon::id, c -> c));

        return userCoupons.map(userCoupon -> {
            Coupon coupon = couponMap.get(userCoupon.couponId());
            return UserCouponResponse.from(userCoupon, coupon);
        });
    }
    /**
     * 사용 가능한 쿠폰 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<UserCouponResponse> getAvailableCoupons(Long userId, Pageable pageable) {
        Page<UserCoupon> userCoupons = userCouponRepository
            .findByUserIdAndStatus(userId, UserCouponStatus.AVAILABLE, pageable);
        // 1. 먼저 모든 couponId 수집
        List<Long> couponIds = userCoupons.stream()
            .map(UserCoupon::couponId)
            .toList();
        // 2. 한번에 조회
        Map<Long, Coupon> couponMap = couponRepository.findAllById(couponIds)
            .stream()
            .collect(Collectors.toMap(Coupon::id, c -> c));
        // 3. map에서 조회
        return userCoupons.map(userCoupon -> {
            Coupon coupon = couponMap.get(userCoupon.couponId());
            return UserCouponResponse.from(userCoupon, coupon);
        });
    }

    /**
     *사용자 쿠폰 조회
     */
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long userId, Long couponId) {
        return couponRepository.findById(couponId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, couponId));
    }

    /**
     * 쿠폰 사용 처리
     */
    public void useCoupon(Long userId, Long couponId) {
        // 사용자의 사용 가능한 쿠폰 조회
        UserCoupon userCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndStatus(userId, couponId, UserCouponStatus.AVAILABLE)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.COUPON_NOT_FOUND, 
                userId, 
                couponId
            ));
        
        // 쿠폰 사용 처리
        UserCoupon usedCoupon = userCoupon.use();
        userCouponRepository.save(usedCoupon);
        
    }

    /**
     * 쿠폰 복구 (주문 취소 시)
     */
    public void restoreCoupon(Long userId, Long couponId) {
        // 사용된 쿠폰 조회
        UserCoupon userCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndStatus(userId, couponId, UserCouponStatus.USED)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.COUPON_NOT_FOUND, 
                userId, 
                couponId
            ));
        
        // 쿠폰 복구 (AVAILABLE 상태로 변경)
        UserCoupon restoredCoupon = userCoupon.restore();
        userCouponRepository.save(restoredCoupon);
    }
}
