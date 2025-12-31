package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return userCouponJpaRepository.existsByUserIdAndCouponId(userId, couponId);
    }

    @Override
    public List<UserCoupon> findAllByCouponId(Long couponId) {
        return userCouponJpaRepository.findByCouponId(couponId).stream()
                .map(UserCouponEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponIdAndStatus(Long userId, Long couponId, UserCouponStatus status) {
        return userCouponJpaRepository.findByUserIdAndCouponIdAndStatus(userId, couponId, status)
                .map(UserCouponEntity::toDomain);
    }

    @Override
    public Page<UserCoupon> findByUserId(Long userId, Pageable pageable) {
        return userCouponJpaRepository.findByUserId(userId, pageable)
                .map(UserCouponEntity::toDomain);
    }

    @Override
    public Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable) {
        return userCouponJpaRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(UserCouponEntity::toDomain);
    }
}