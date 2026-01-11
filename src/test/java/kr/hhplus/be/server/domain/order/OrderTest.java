package kr.hhplus.be.server.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.support.exception.BusinessException;

class OrderTest {

    @Test
    @DisplayName("성공: 주문 생성 시 초기 상태는 PENDING이다")
    void 초기_상태_확인() {
        // when
        Order order = Order.create(1L, 10L, 50000L, 5000L, 45000L);

        // then
        assertThat(order.orderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("실패: 최종 금액이 0원 미만인 주문 생성 시 예외가 발생한다")
    void 최종금액_주문_실패() {
        // when & then
        assertThatThrownBy(() -> Order.create(1L, 10L, 5000L, 10000L, -5000L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("성공: 주문 취소는 PENDING이나 COMPLETED 상태에서 가능하다")
    void 주문_취소_상태_확인() {
        // given
        Order order = Order.create(1L, 10L, 50000L, 0L, 50000L).complete();

        // when
        Order canceledOrder = order.cancel();

        // then
        assertThat(canceledOrder.orderStatus()).isEqualTo(OrderStatus.CANCELED);
    }
}