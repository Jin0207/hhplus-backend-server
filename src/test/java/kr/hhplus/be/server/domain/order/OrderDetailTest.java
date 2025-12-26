package kr.hhplus.be.server.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.support.exception.BusinessException;

class OrderDetailTest {

    @Test
    @DisplayName("성공: 주문 상세 생성 시 소계가 올바르게 계산된다")
    void 소계_계산() {
        // given
        Long orderId = 1L;
        Long productId = 100L;
        Integer quantity = 3;
        Long unitPrice = 15000L;

        // when
        OrderDetail detail = OrderDetail.create(orderId, productId, quantity, unitPrice);

        // then
        assertThat(detail.subtotal()).isEqualTo(45000L); // 3 * 15000
        assertThat(detail.orderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("실패: 수량이 0 이하일 경우 주문 상세 생성 시 예외가 발생한다")
    void 수량_없음_주문_실패() {
        // given
        Long orderId = 1L;
        Integer quantity = 0;

        // when & then
        assertThatThrownBy(() -> OrderDetail.create(orderId, 100L, quantity, 15000L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("성공: assignOrderId를 통해 새로운 주문 ID가 부여된 객체를 생성한다")
    void 주문_아이디_부여() {
        // given
        OrderDetail detail = OrderDetail.create(null, 100L, 2, 1000L);
        Long newOrderId = 999L;

        // when
        OrderDetail assignedDetail = detail.assignOrderId(newOrderId);

        // then
        assertThat(assignedDetail.orderId()).isEqualTo(newOrderId);
        assertThat(assignedDetail.productId()).isEqualTo(detail.productId());
    }
}