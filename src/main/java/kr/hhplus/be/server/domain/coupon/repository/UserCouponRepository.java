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
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
