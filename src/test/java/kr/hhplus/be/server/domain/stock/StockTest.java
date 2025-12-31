package kr.hhplus.be.server.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.enums.StockType;

class StockTest {

    @Test
    @DisplayName("성공: 출고(OUT) 이력 생성")
    void 출고_이력_생성() {
        // given
        Long productId = 50L;
        Integer quantity = 10;
        String reason = "주문 출고";

        // when
        Stock stock = Stock.createOut(productId, quantity, reason);

        // then
        assertThat(stock.stockType()).isEqualTo(StockType.OUT);
        assertThat(stock.quantity()).isEqualTo(quantity);
        assertThat(stock.productId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("성공: 입고(IN) 이력 생성")
    void 입고_이력_생성() {
        // given
        Long productId = 50L;
        Integer quantity = 100;

        // when
        Stock stock = Stock.createIn(productId, quantity, "재고 매입");

        // then
        assertThat(stock.stockType()).isEqualTo(StockType.IN);
    }
}
