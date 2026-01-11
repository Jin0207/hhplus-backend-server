package kr.hhplus.be.server.domain.coupon.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;

public interface CouponRepository {
    Optional<Coupon> findById(Long id);
    Optional<Coupon> findByIdWithLock(Long id);
    List<Coupon> findAllById(Iterable<Long> ids);
    Coupon save(Coupon coupon);
}
