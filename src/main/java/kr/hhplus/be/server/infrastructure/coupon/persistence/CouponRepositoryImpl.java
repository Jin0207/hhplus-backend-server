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
    
    @Override
    public List<Coupon> findAllById(Iterable<Long> ids) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Optional<Coupon> findById(Long id) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public Optional<Coupon> findByIdWithLock(Long id) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public Coupon save(Coupon coupon) {
        // TODO Auto-generated method stub
        return null;
    }
}
