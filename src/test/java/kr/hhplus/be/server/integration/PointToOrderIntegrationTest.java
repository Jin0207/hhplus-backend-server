package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;

/**
 * 포인트 충전 → 주문까지 전체 플로우 통합 테스트
 */
class PointToOrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("✅ 포인트 충전 → 주문 → 잔액 차감 전체 플로우 테스트")
    void chargePointAndOrderFlow() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("pointorder_" + timestamp + "@example.com", "password123"));

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                50,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                null,
                null
            )
        );

        // Given: 초기 포인트 확인
        Long initialBalance = pointService.getPointBalance(testUser.id());
        assertThat(initialBalance).isEqualTo(0L);

        // When 1: 포인트 충전 (200,000원)
        Long chargeAmount = 200000L;
        pointService.chargePoint(testUser.id(), chargeAmount, "테스트 충전");

        // Then 1: 충전 후 잔액 확인
        Long balanceAfterCharge = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterCharge).isEqualTo(chargeAmount);

        // When 2: 상품 주문 (100,000원 * 1개)
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null, // 쿠폰 미사용
            0L, // 포인트 사용 안함
            "POINT",
            UUID.randomUUID().toString()
        );

        OrderResponse orderResponse = orderFacade.completeOrder(testUser.id(), orderRequest);

        // Then 2: 주문 성공 확인
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isNotNull();
        assertThat(orderResponse.finalPrice()).isEqualTo(100000L);

        // Then 3: 잔액 차감 확인
        Long balanceAfterOrder = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterOrder).isEqualTo(100000L); // 200,000 - 100,000 = 100,000

        // Then 4: 재고 차감 확인
        Product updatedProduct = productRepository.findById(testProduct.id()).orElseThrow();
        assertThat(updatedProduct.stock()).isEqualTo(49); // 50 - 1 = 49
        assertThat(updatedProduct.salesQuantity()).isEqualTo(1); // 판매량 증가
    }

    @Test
    @DisplayName("✅ 포인트 부족 시 주문 실패 테스트")
    void orderFailsWhenInsufficientPoints() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("insufficient_" + timestamp + "@example.com", "password123"));

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                50,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                null,
                null
            )
        );

        // Given: 포인트 충전 (50,000원 - 부족한 금액)
        pointService.chargePoint(testUser.id(), 50000L, "테스트 충전");

        // When & Then: 주문 시도 시 예외 발생
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        org.junit.jupiter.api.Assertions.assertThrows(
            kr.hhplus.be.server.support.exception.BusinessException.class,
            () -> orderFacade.completeOrder(testUser.id(), orderRequest)
        );

        // Then: 잔액 변동 없음 (롤백 확인)
        Long balanceAfterFailedOrder = pointService.getPointBalance(testUser.id());
        assertThat(balanceAfterFailedOrder).isEqualTo(50000L);

        // Then: 재고 변동 없음 (롤백 확인)
        Product unchangedProduct = productRepository.findById(testProduct.id()).orElseThrow();
        assertThat(unchangedProduct.stock()).isEqualTo(50);
        assertThat(unchangedProduct.salesQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("✅ 여러 번 충전 후 주문 테스트")
    void multipleChargesAndOrder() {
        // Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User testUser = userRepository.save(User.create("multiple_" + timestamp + "@example.com", "password123"));

        Product testProduct = productRepository.save(
            new Product(
                null,
                "테스트 상품",
                100000L,
                50,
                ProductCategory.TOP,
                ProductStatus.ON_SALE,
                0,
                null,
                null
            )
        );

        // When: 3번에 걸쳐 충전
        pointService.chargePoint(testUser.id(), 50000L, "1차 충전");
        pointService.chargePoint(testUser.id(), 30000L, "2차 충전");
        pointService.chargePoint(testUser.id(), 20000L, "3차 충전");

        // Then: 누적 잔액 확인
        Long totalBalance = pointService.getPointBalance(testUser.id());
        assertThat(totalBalance).isEqualTo(100000L);

        // When: 주문
        OrderCreateRequest orderRequest = new OrderCreateRequest(
            List.of(new OrderCreateRequest.OrderItem(testProduct.id(), 1)),
            null,
            0L,
            "POINT",
            UUID.randomUUID().toString()
        );

        orderFacade.completeOrder(testUser.id(), orderRequest);

        // Then: 최종 잔액 확인
        Long finalBalance = pointService.getPointBalance(testUser.id());
        assertThat(finalBalance).isEqualTo(0L);
    }
}
