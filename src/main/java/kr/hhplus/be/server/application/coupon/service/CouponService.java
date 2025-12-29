package kr.hhplus.be.server.application.coupon.service;

import java.time.Duration;
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

    private static final String COUPON_ISSUE_KEY = "coupon:issue:";
    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";

    /**
     * 선착순 쿠폰 발급
     */
    @Transactional
    public UserCouponResponse issueCoupon(Long userId, Long couponId) {
        String issueKey = COUPON_ISSUE_KEY + couponId + ":user:" + userId;
        String quantityKey = COUPON_QUANTITY_KEY + couponId;

        // 1. Redis 중복 발급 체크
        Boolean alreadyIssued = redisTemplate.opsForValue()
            .setIfAbsent(issueKey, "1", Duration.ofDays(1));

        if (Boolean.FALSE.equals(alreadyIssued)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        try {
            // 2. Redis 재고 차감
            Long remainingQuantity = redisTemplate.opsForValue().decrement(quantityKey);

            if (remainingQuantity == null || remainingQuantity < 0) {
                throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            // 3. DB 처리
            Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            if (!coupon.canIssue()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            UserCoupon userCoupon = UserCoupon.issue(userId, couponId, coupon.validTo());
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            Coupon updatedCoupon = coupon.decreaseQuantity();
            couponRepository.save(updatedCoupon);

            log.info("쿠폰 발급 성공: userId={}, couponId={}, remaining={}",
                userId, couponId, remainingQuantity);

            return UserCouponResponse.from(savedUserCoupon, coupon);

        } catch (BusinessException e) {
            rollbackRedis(quantityKey, issueKey);
            throw e;
        } catch (Exception e) {
            rollbackRedis(quantityKey, issueKey);
            throw new BusinessException(ErrorCode.COUPON_ISSUE_FAILED, e);
        }
    }

    /**
     * Redis 롤백 (재고 복구 및 발급 키 삭제)
     */
    private void rollbackRedis(String quantityKey, String issueKey) {
        try {
            redisTemplate.opsForValue().increment(quantityKey);
            redisTemplate.delete(issueKey);
            log.warn("Redis 롤백 완료: quantityKey={}, issueKey={}", quantityKey, issueKey);
        } catch (Exception e) {
            log.error("Redis 롤백 실패: quantityKey={}, issueKey={}", quantityKey, issueKey, e);
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
