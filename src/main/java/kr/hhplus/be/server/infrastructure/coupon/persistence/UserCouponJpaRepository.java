package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;

public interface UserCouponJpaRepository extends JpaRepository<UserCouponEntity, UserCouponId>{
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    List<UserCouponEntity> findByCouponId(Long couponId);

    Optional<UserCouponEntity> findByUserIdAndCouponIdAndStatus(Long userId, Long couponId, UserCouponStatus status);

    Page<UserCouponEntity> findByUserId(Long userId, Pageable pageable);

    Page<UserCouponEntity> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);
}
