package kr.hhplus.be.server.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest.OrderItem;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.integration.BaseIntegrationTest;
import kr.hhplus.be.server.domain.outbox.entity.OutBoxMessage;
import kr.hhplus.be.server.domain.outbox.repository.OutBoxMessageRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;

class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OutBoxMessageRepository outBoxMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointService pointService;

    @Test
    @DisplayName("✅ 주문 및 결제 완료 시 Outbox 메시지 원자적 저장 테스트")
    void completeOrderWithOutboxTest() {
        // 1. Given: 테스트 데이터 생성 (타임스탬프로 고유성 보장)
        long timestamp = System.currentTimeMillis();
        User user = userRepository.save(User.create("orderintegration_" + timestamp + "@test.com", "1234"));

        // 2. 테스트용 상품 생성 (재고 100개)
        Product product = productRepository.save(
            new Product(null, "테스트 상품", 10000L, 100,
                    ProductCategory.SHOESE, ProductStatus.ON_SALE,
                    0, LocalDateTime.now(), null)
        );

        // 3. 사용자에게 포인트 충전
        pointService.chargePoint(user.id(), 50000L, "테스트 충전");

        // 4. 주문 요청 생성
        OrderCreateRequest request = new OrderCreateRequest(
            List.of(new OrderItem(product.id(), 2)), // 2개 주문
            null, // 쿠폰 없음
            0L, // 포인트 사용 안함 (전액 포인트 결제)
            "POINT", // 포인트 결제
            UUID.randomUUID().toString() // 멱등성 키
        );

        // 2. When: 주문 완료 실행 (OrderFacade가 @Transactional로 처리)
        OrderResponse response = orderFacade.completeOrder(user.id(), request);

        // 3. Then: Outbox 저장 여부 검증
        List<OutBoxMessage> messages = outBoxMessageRepository.findPendingMessages(10);

        // Outbox 메시지 검증
        assertThat(messages).anyMatch(msg ->
            msg.aggregateId().equals(response.orderId()) &&
            msg.eventType().equals("ORDER_COMPLETED")
        );

        // 4. Then: 데이터 무결성 확인
        OutBoxMessage savedMsg = messages.stream()
                .filter(m -> m.aggregateId().equals(response.orderId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Outbox 메시지가 저장되지 않았습니다"));

        assertThat(savedMsg.isProcessed()).isFalse(); // 스케줄러 처리 전이므로 false가 맞음
        assertThat(savedMsg.eventType()).isEqualTo("ORDER_COMPLETED");
    }
}
