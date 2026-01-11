package kr.hhplus.be.server.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.support.exception.BusinessException;

class UserTest {

    @Test
    @DisplayName("성공: 포인트 충전에 성공하고 새로운 유저 객체를 반환한다")
    void 포인트_충전() {
        // given
        User user = User.create("testUser", "password123");
        Long chargeAmount = 50000L;

        // when
        User updatedUser = user.chargePoint(chargeAmount);

        // then
        assertThat(updatedUser.point()).isEqualTo(50000L);
        assertThat(user.point()).isEqualTo(0L); // 원본 객체는 유지(불변)
    }

    @Test
    @DisplayName("실패: 최대 보유 포인트(100만) 초과 충전 시 예외가 발생한다")
    void 최대_보유_포인트_초과() {
        // given
        User user = User.create("testUser", "password123").chargePoint(900000L);
        Long chargeAmount = 200000L;

        // when & then
        assertThatThrownBy(() -> user.chargePoint(chargeAmount))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("성공: 포인트 사용에 성공한다")
    void 포인트_사용() {
        // given
        User user = User.create("testUser", "password123").chargePoint(10000L);
        Long useAmount = 3000L;

        // when
        User updatedUser = user.usePoint(useAmount);

        // then
        assertThat(updatedUser.point()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("실패: 보유 포인트보다 많은 금액 사용 시 예외가 발생한다")
    void 보유_포인트_적음() {
        // given
        User user = User.create("testUser", "password123").chargePoint(1000L);

        // when & then
        assertThatThrownBy(() -> user.usePoint(5000L))
            .isInstanceOf(BusinessException.class);
    }
}