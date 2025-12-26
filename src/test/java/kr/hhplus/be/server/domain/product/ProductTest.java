package kr.hhplus.be.server.domain.product;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.support.exception.BusinessException;

class ProductTest {

    @Test
    @DisplayName("성공: 재고 차감 시 수량이 줄어들고 0이 되면 SOLD_OUT 상태가 된다")
    void 재고_차감_품절() {
        // given
        Product product = new Product(1L, "구찌신발", 1000000L, 5, 
            ProductCategory.SHOESE, ProductStatus.ON_SALE, 0, LocalDateTime.now(), null);

        // when
        Product updatedProduct = product.decreaseStock(5);

        // then
        assertThat(updatedProduct.stock()).isEqualTo(0);
        assertThat(updatedProduct.status()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("실패: 재고보다 많은 수량 차감 시 예외가 발생한다")
    void 수량_재고_보다_많음() {
        // given
        Product product = new Product(1L, "구찌신발", 1000000L, 3, 
            ProductCategory.SHOESE, ProductStatus.ON_SALE, 0, LocalDateTime.now(), null);

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(5))
            .isInstanceOf(BusinessException.class);
    }
}