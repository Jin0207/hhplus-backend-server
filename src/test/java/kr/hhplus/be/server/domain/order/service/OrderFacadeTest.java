package kr.hhplus.be.server.domain.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest.OrderItem;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.application.order.service.OrderService;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentStatus;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@DisplayName("OrderFacade TDD Test")
public class OrderFacadeTest {

        @InjectMocks
        private OrderFacade orderFacade;

        @Mock
        private OrderService orderService;
        @Mock
        private PaymentService paymentService;
        @Mock
        private ProductService productService;
        @Mock
        private UserService userService;

        // 공통 테스트 데이터
        private Long userId = 1L;
        private Long productId = 10L;
        private Integer productPrice = 5000;
        private Integer initialProductStock = 50;

        private OrderCreateRequest request;
        private Product product;
        private Order createdOrder;
        private Payment createdPayment;
        private Order completedOrder;
        private Payment completedPayment;

        @BeforeEach
        void setUp() {
                // 1. OrderCreateRequest 설정 (상품 1개, 2개 구매)
                OrderItem orderItem = new OrderItem(productId, 2);
                request = new OrderCreateRequest(
                        List.of(orderItem), 
                        null, 
                        0, 
                        0, // 포인트 미사용
                        PaymentType.CARD.name()
                );

                // 2. Product 설정
                product = new Product(
                        productId, 
                        "테스트 상품", 
                        productPrice, 
                        initialProductStock, 
                        ProductCategory.TOP, 
                        ProductStatus.ON_SALE, 
                        10, 
                        LocalDateTime.now(), 
                        null
                );

                // 3. Mock Order/Payment/User 객체 설정
                createdOrder = new Order(
                        200L, userId, null, 10000, 0, 10000, 
                        OrderStatus.PENDING, LocalDateTime.now(), null
                );
                createdPayment = new Payment(
                        300L, 200L, userId, 10000, 
                        PaymentStatus.PENDING, PaymentType.CARD, "PG", null, null, 
                        LocalDateTime.now(), null, false, null, LocalDateTime.now(), null
                );
                completedOrder = createdOrder.complete();
                completedPayment = createdPayment.complete(createdPayment, "TXN_MOCK_ID");
        }

        // --- 1. 정상 주문/결제 테스트 (쿠폰, 포인트 없음) ---
        @Test
        @DisplayName("정상 주문 및 결제 완료 성공")
        void 주문_결제_완료() {

                int orderQuantity = 2;
                int expectedStockAfterDeduction = initialProductStock - orderQuantity;
                Product productAfterDeduction = product.decreaseStock(orderQuantity);

                // Mocking 설정
                // 1. 재고 확인 및 차감 (processStockDeduction)
                when(productService.getProduct(productId)).thenReturn(product);
                when(productService.decreaseStock(eq(productId), eq(orderQuantity)))
                        .thenReturn(productAfterDeduction); 

                // 2. 포인트 차감 (포인트 0이므로 호출 안 됨. verify에서 확인)

                // 3. 주문 생성 (OrderService)
                when(orderService.createOrder(eq(userId), eq(null), eq(10000), eq(0), eq(10000), any()))
                        .thenReturn(createdOrder);

                // 4. 결제 생성 (PaymentService)
                when(paymentService.createPayment(eq(createdOrder.id()), eq(userId), eq(10000), eq(request.paymentType())))
                        .thenReturn(createdPayment);

                // 5. 결제 완료 처리
                when(paymentService.completePayment(eq(createdPayment.id()), anyString()))
                        .thenReturn(completedPayment);

                // 6. 주문 완료 처리
                when(orderService.completeOrder(eq(createdOrder.id()))).thenReturn(completedOrder);

                // 7. 판매량 증가 (increaseSalesQuantity)
                Product productAfterSalesIncrease = productAfterDeduction.increaseSalesQuantity(orderQuantity);
                when(productService.increaseSalesQuantity(eq(productId), eq(orderQuantity)))
                        .thenReturn(productAfterSalesIncrease);

                // Act
                OrderResponse response = orderFacade.createOrderWithPayment(userId, request);

                // Assert
                assertNotNull(response);
                assertEquals(createdOrder.id(), response.orderId());
                assertEquals(createdOrder.finalPrice(), response.finalPrice());

                // 주요 서비스 호출 검증 (호출 횟수 및 인자)
                verify(productService, times(2)).getProduct(productId);
                verify(productService, times(1)).decreaseStock(productId, orderQuantity);
                verify(userService, times(0)).usePoint(anyLong(), anyInt(), anyString()); // 포인트 0이므로 호출되면 안 됨
                verify(orderService, times(1)).createOrder(anyLong(), any(), anyInt(), anyInt(), anyInt(), any());
                verify(paymentService, times(1)).createPayment(anyLong(), anyLong(), anyInt(), anyString());
                verify(paymentService, times(1)).completePayment(eq(createdPayment.id()), anyString());
                verify(orderService, times(1)).completeOrder(createdOrder.id());
                verify(productService, times(1)).increaseSalesQuantity(productId, orderQuantity);
        }

        // --- 2. 포인트 사용 주문/결제 테스트 ---
        @Test
        @DisplayName("포인트 사용 포함 주문 및 결제 완료 성공")
        void 주문_결제_완료_포인트_사용() {
                int pointToUse = 500;
                int totalPrice = 10000;
                int expectedFinalPrice = totalPrice - pointToUse; // 할인 0, 포인트 500

                OrderItem orderItem = new OrderItem(productId, 2);
                OrderCreateRequest requestWithPoint = new OrderCreateRequest(
                        List.of(orderItem), 
                        null, 
                        0, 
                        pointToUse, 
                        PaymentType.CARD.name()
                );

                // 포인트 사용 전/후 User Mock 설정
                User userBeforeUse = new User(userId, "testId", "pwd", 1000, LocalDateTime.now(), null);
                User userAfterUse = userBeforeUse.usePoint(pointToUse);

                Order orderWithPoint = new Order(
                        201L, userId, null, totalPrice, 0, expectedFinalPrice, 
                        OrderStatus.PENDING, LocalDateTime.now(), null
                );
                Payment paymentWithPoint = new Payment(
                        301L, 201L, userId, expectedFinalPrice, 
                        PaymentStatus.PENDING, PaymentType.CARD, "PG", null, null, 
                        LocalDateTime.now(), null, false, null, LocalDateTime.now(), null
                );
                Order completedOrderWithPoint = orderWithPoint.complete();
                Payment completedPaymentWithPoint = paymentWithPoint.complete(paymentWithPoint, "TXN_MOCK_ID_PT");

                // Mocking 설정
                // 1. 재고 확인 및 차감
                when(productService.getProduct(productId)).thenReturn(product);
                when(productService.decreaseStock(eq(productId), anyInt()))
                        .thenReturn(product.decreaseStock(2)); 

                // 2. 포인트 차감
                when(userService.usePoint(eq(userId), eq(pointToUse), anyString()))
                        .thenReturn(userAfterUse);

                // 3. 주문 생성 (최종 금액 9500원 확인)
                when(orderService.createOrder(eq(userId), eq(null), eq(totalPrice), eq(0), eq(expectedFinalPrice), any()))
                        .thenReturn(orderWithPoint);

                // 4. 결제 생성 (최종 금액 9500원 확인)
                when(paymentService.createPayment(eq(orderWithPoint.id()), eq(userId), eq(expectedFinalPrice), eq(requestWithPoint.paymentType())))
                        .thenReturn(paymentWithPoint);

                // 5. 결제 완료 처리
                when(paymentService.completePayment(eq(paymentWithPoint.id()), anyString()))
                        .thenReturn(completedPaymentWithPoint);

                // 6. 주문 완료 처리
                when(orderService.completeOrder(eq(orderWithPoint.id()))).thenReturn(completedOrderWithPoint);

                // 7. 판매량 증가
                when(productService.increaseSalesQuantity(eq(productId), anyInt()))
                        .thenReturn(product.decreaseStock(2).increaseSalesQuantity(2));

                // Act
                OrderResponse response = orderFacade.createOrderWithPayment(userId, requestWithPoint);

                // Assert
                assertNotNull(response);
                assertEquals(orderWithPoint.id(), response.orderId());
                assertEquals(expectedFinalPrice, response.finalPrice());

                // 포인트 서비스 호출 검증
                verify(userService, times(1)).usePoint(userId, pointToUse, "주문 결제");
                verify(orderService, times(1)).createOrder(eq(userId), any(), eq(totalPrice), eq(0), eq(expectedFinalPrice), any());
                verify(paymentService, times(1)).createPayment(anyLong(), anyLong(), eq(expectedFinalPrice), anyString());
        }

        // --- 3. 재고 부족 실패 테스트 ---
        @Test
        @DisplayName("재고 부족 시 BusinessException 발생 및 트랜잭션 롤백 시도")
        void 주문_재고_부족() {
                OrderItem orderItem = new OrderItem(productId, 60); // 재고 50인데 60개 요청
                OrderCreateRequest requestLargeQuantity = new OrderCreateRequest(
                        List.of(orderItem), null, 0, 0, PaymentType.CARD.name()
                );

                // Mocking 설정
                when(productService.getProduct(productId)).thenReturn(product);

                // 재고 부족 예외 확인
                BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderFacade.createOrderWithPayment(userId, requestLargeQuantity);
                });

                assertEquals(ErrorCode.ORDER_STOCK_INSUFFICIENT, exception.getErrorCode());

                // 주문/결제/포인트 차감 서비스는 호출되지 않아야 함
                verify(productService, times(0)).decreaseStock(anyLong(), anyInt()); // 재고 차감은 호출되지 않음
                verify(userService, times(0)).usePoint(anyLong(), anyInt(), anyString()); 
                verify(orderService, times(0)).createOrder(anyLong(), any(), anyInt(), anyInt(), anyInt(), any());
                verify(paymentService, times(0)).createPayment(anyLong(), anyLong(), anyInt(), anyString());
                verify(paymentService, times(0)).completePayment(anyLong(), anyString());
                verify(orderService, times(0)).completeOrder(anyLong());
                verify(productService, times(0)).increaseSalesQuantity(anyLong(), anyInt());
        }

        // --- 4. 외부 결제 실패 테스트 ---
        @Test
        @DisplayName("외부 결제 실패 시 BusinessException 발생 및 롤백 시도")
        void 외부_결제_실패() {
                // PaymentType을 "FAIL"로 설정하여 processExternalPayment에서 예외 발생 유도
                OrderItem orderItem = new OrderItem(productId, 2);
                OrderCreateRequest requestPaymentFail = new OrderCreateRequest(
                        List.of(orderItem), 
                        null, 
                        0, 
                        0, 
                        "FAIL" // processExternalPayment에서 이 값으로 실패를 유도
                );

                // Mocking 설정
                // 1. 재고 확인 및 차감
                when(productService.getProduct(productId)).thenReturn(product);
                when(productService.decreaseStock(eq(productId), anyInt()))
                .thenReturn(product.decreaseStock(2)); 

                // 2. 주문 생성
                when(orderService.createOrder(anyLong(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(createdOrder);

                // 3. 결제 생성
                when(paymentService.createPayment(anyLong(), anyLong(), anyInt(), eq("FAIL")))
                .thenReturn(createdPayment);

                // processExternalPayment에서 BusinessException(ErrorCode.PAYMENT_FAILED)이 발생
                // 결제 실패 예외 확인
                BusinessException exception = assertThrows(BusinessException.class, () -> {
                        orderFacade.createOrderWithPayment(userId, requestPaymentFail);
                });

                assertEquals(ErrorCode.PAYMENT_FAILED, exception.getErrorCode());

                // 주문 생성 및 결제 생성은 호출되었으나,
                // 결제 완료, 주문 완료, 판매량 증가는 호출되지 않아야 함
                verify(productService, times(1)).decreaseStock(anyLong(), anyInt()); // 재고는 차감됨 (롤백되어야 함)
                verify(orderService, times(1)).createOrder(anyLong(), any(), anyInt(), anyInt(), anyInt(), any());
                verify(paymentService, times(1)).createPayment(anyLong(), anyLong(), anyInt(), anyString());

                verify(paymentService, times(0)).completePayment(anyLong(), anyString()); // 결제 완료 호출되면 안 됨
                verify(orderService, times(0)).completeOrder(anyLong()); 
                verify(productService, times(0)).increaseSalesQuantity(anyLong(), anyInt()); 
        }
}