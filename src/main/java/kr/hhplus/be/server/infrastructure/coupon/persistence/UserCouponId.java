package kr.hhplus.be.server.infrastructure.coupon.persistence;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserCouponId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long couponId;
}