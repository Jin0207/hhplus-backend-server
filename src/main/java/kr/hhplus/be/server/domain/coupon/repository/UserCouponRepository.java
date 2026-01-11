package kr.hhplus.be.server.domain.coupon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon userCoupon);
    Page<UserCoupon> findByUserId(Long userI, Pageable pageable);
    Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    /**
     * 특정 쿠폰의 모든 발급 내역 조회
     */
    List<UserCoupon> findAllByCouponId(Long couponId);

    /**
     * 사용자의 특정 쿠폰 조회 (상태별)
     */
    Optional<UserCoupon> findByUserIdAndCouponIdAndStatus(
        Long userId,
        Long couponId,
        UserCouponStatus status
    );
}
