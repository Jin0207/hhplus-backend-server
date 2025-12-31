package kr.hhplus.be.server.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.coupon.entity.UserCoupon;
import kr.hhplus.be.server.domain.coupon.enums.UserCouponStatus;
import kr.hhplus.be.server.support.exception.BusinessException;

class UserCouponTest {

    @Test
    @DisplayName("성공: 쿠폰 사용 시 상태가 USED로 변경되고 사용일시가 기록된다")
    void 쿠폰_사용_상태_변경() {
        // given
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(7);
        UserCoupon coupon = UserCoupon.issue(1L, 100L, expiredAt);

        // when
        UserCoupon usedCoupon = coupon.use();

        // then
        assertThat(usedCoupon.status()).isEqualTo(UserCouponStatus.USED);
        assertThat(usedCoupon.usedDttm()).isNotNull();
    }

    @Test
    @DisplayName("실패: 만료된 쿠폰 사용 시 예외가 발생한다")
    void 쿠폰_만료_실패() {
        // given
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(1);
        UserCoupon coupon = new UserCoupon(1L, 100L, UserCouponStatus.AVAILABLE, 
            null, expiredAt, LocalDateTime.now(), null);

        // when & then
        assertThatThrownBy(() -> coupon.use())
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("성공: 사용된 쿠폰을 복구(restore)하면 AVAILABLE 상태가 된다")
    void 쿠폰_복구_상태_변경() {
        // given
        UserCoupon usedCoupon = new UserCoupon(1L, 100L, UserCouponStatus.USED, 
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now(), null);

        // when
        UserCoupon restored = usedCoupon.restore();

        // then
        assertThat(restored.status()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(restored.usedDttm()).isNull();
    }
}
