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
    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Page<UserCoupon> findByUserId(Long userI, Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }
}
