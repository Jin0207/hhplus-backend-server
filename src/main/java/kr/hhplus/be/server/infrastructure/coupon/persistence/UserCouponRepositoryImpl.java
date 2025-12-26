package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepository{
    
    private final UserCouponJpaRepository userCouponJpaRepository;

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        UserCouponEntity entity = UserCouponEntity.from(userCoupon);
        return userCouponJpaRepository.save(entity).toDomain();
    }
    
    @Override
    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        return userCouponJpaRepository.existsByIdUserIdAndIdCouponId(userId, couponId);
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return userCouponJpaRepository.findByIdUserIdAndIdCouponId(userId, couponId)
                .map(UserCouponEntity::toDomain);
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponIdAndStatus(Long userId, Long couponId, UserCouponStatus status) {
        return userCouponJpaRepository.findByIdUserIdAndIdCouponIdAndStatus(userId, couponId, status)
                .map(UserCouponEntity::toDomain);
    }

    @Override
    public Page<UserCoupon> findByUserId(Long userId, Pageable pageable) {
        return userCouponJpaRepository.findByIdUserId(userId, pageable)
                .map(UserCouponEntity::toDomain);
    }

    @Override
    public Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable) {
        return userCouponJpaRepository.findByIdUserIdAndStatus(userId, status, pageable)
                .map(UserCouponEntity::toDomain);
    }
}