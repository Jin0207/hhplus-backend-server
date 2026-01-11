package kr.hhplus.be.server.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.enums.CouponStatus;
import kr.hhplus.be.server.domain.coupon.enums.CouponType;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;


class CouponTest {

    private Coupon createBaseCoupon(Long discount, Long minPrice, CouponType type) {
        return new Coupon(
            1L, "테스트 쿠폰", type, discount, minPrice,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
            100, 10, CouponStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("성공: 금액 할인 계산 시 주문금액보다 할인액이 작으면 할인액을 그대로 반환한다.")
    void 고정_금액_할인_계산() {
        // given
        Coupon coupon = createBaseCoupon(5000L, 10000L, CouponType.AMOUNT);

        // when
        Long discount = coupon.calculateDiscountAmount(15000L);

        // then
        assertThat(discount).isEqualTo(5000L);
    }

    @Test
    @DisplayName("성공: 퍼센트(PERCENT) 할인 계산 시 주문금액의 비율만큼 할인이 적용된다.")
    void 퍼센트_할인_계산() {
        // given
        Coupon coupon = createBaseCoupon(10L, 10000L, CouponType.PERCENT);

        // when
        Long discount = coupon.calculateDiscountAmount(20000L);

        // then
        assertThat(discount).isEqualTo(2000L); // 20,000의 10%
    }

    @Test
    @DisplayName("실패: 주문 금액이 최소 주문 금액보다 작으면 예외가 발생한다.")
    void 최소_주문_금액_미달_실패() {
        // given
        Coupon coupon = createBaseCoupon(5000L, 10000L, CouponType.AMOUNT);

        // when & then
        assertThatThrownBy(() -> coupon.calculateDiscountAmount(9000L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COUPON_MIN_ORDER_PRICE_NOT_MET);
    }

    @Test
    @DisplayName("성공: 수량 차감 시 기존 객체는 유지되고 남은 수량이 1 감소한 새 객체를 반환한다.")
    void 수량_차감_불변성_테스트() {
        // given
        Coupon coupon = createBaseCoupon(5000L, 10000L, CouponType.AMOUNT);
        int originalQuantity = coupon.availableQuantity();

        // when
        Coupon updatedCoupon = coupon.decreaseQuantity();

        // then
        assertThat(updatedCoupon.availableQuantity()).isEqualTo(originalQuantity - 1);
        assertThat(coupon.availableQuantity()).isEqualTo(originalQuantity); // 원본 불변 유지
        assertThat(updatedCoupon).isNotEqualTo(coupon);
    }

    @Test
    @DisplayName("성공: 상태가 ACTIVE이고 기간 내에 있으며 재고가 있으면 발급 가능하다.")
    void 발급_가능_여부_확인() {
        // given
        Coupon coupon = createBaseCoupon(5000L, 10000L, CouponType.AMOUNT);

        // when & then
        assertThat(coupon.canIssue()).isTrue();
    }
}