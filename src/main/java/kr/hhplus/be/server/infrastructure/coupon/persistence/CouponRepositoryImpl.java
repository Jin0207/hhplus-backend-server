package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository{
    
    private final CouponJpaRepository couponJpaRepository;

    @Override
    public List<Coupon> findAllById(Iterable<Long> ids) {
        return couponJpaRepository.findAllById(ids).stream().map(CouponEntity::toDomain).toList();
    }
    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id).map(CouponEntity::toDomain);
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity entity = CouponEntity.from(coupon);
        return couponJpaRepository.save(entity).toDomain();
    }
}
