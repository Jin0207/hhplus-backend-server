package kr.hhplus.be.server.domain.point;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.enums.PointType;

class PointHistoryTest {

    @Test
    @DisplayName("성공: 포인트 이력 생성 시 금액과 타입이 기록된다")
    void 포인트_이력_생성() {
        // given
        Long userId = 1L;
        Long amount = 5000L;
        PointType type = PointType.CHARGE;
        String comment = "포인트 충전";

        // when
        PointHistory history = PointHistory.insert(userId, amount, type, comment);

        // then
        assertThat(history.userId()).isEqualTo(userId);
        assertThat(history.point()).isEqualTo(amount);
        assertThat(history.type()).isEqualTo(type);
        assertThat(history.crtDttm()).isNotNull();
    }
}