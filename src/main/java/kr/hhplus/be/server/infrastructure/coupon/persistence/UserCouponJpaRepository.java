package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.domain.user.entity.User;

public interface UserCouponJpaRepository extends JpaRepository<UserCouponEntity, UserCouponId>{
    boolean existsByIdUserIdAndIdCouponId(Long userId, Long couponId);
    
    Optional<UserCouponEntity> findByIdUserIdAndIdCouponId(Long userId, Long couponId);
    
    Optional<UserCouponEntity> findByIdUserIdAndIdCouponIdAndStatus(Long userId, Long couponId, UserCouponStatus status);

    Page<UserCouponEntity> findByIdUserId(Long userId, Pageable pageable);

    Page<UserCouponEntity> findByIdUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);
}
