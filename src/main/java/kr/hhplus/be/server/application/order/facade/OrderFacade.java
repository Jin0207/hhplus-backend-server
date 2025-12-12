package kr.hhplus.be.server.application.order.facade;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;ata;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.service.OrderService;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.enums.PaymentType;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final UserService userService;

    /**
     * 주문 및 결제 초기화, 멱등성 보장
     */
    @Transactional
    public OrderAndPaymentData initOrder(Long userId, OrderCreateRequest request){
        // 1. 재고 확인 및 차감
        List<OrderDetail> orderDetails = processStockDeduction(request.items());

        // 2. 총 금액 계산 및 포인트 차감
        Integer totalPrice = calculateTotalPrice(orderDetails);
        Integer discountPrice = request.discountPrice() != null ? request.discountPrice() : 0;
        Integer finalPrice = totalPrice - discountPrice;

        if (request.pointToUse() != null && request.pointToUse() > 0) {
            userService.usePoint(userId, request.pointToUse(), "주문 결제");
            finalPrice -= request.pointToUse();
        }

        // 4. 주문 생성
        Order order = orderService.createOrder(
            userId,
            request.couponId(),
            totalPrice,
            discountPrice,
            finalPrice,
            orderDetails
        );
        
        // 5. 결제 생성 및 처리
        Payment payment = paymentService.createPayment(
            order.id(),
            userId,
            request.idempotencyKey(),
            finalPrice,
            request.paymentType()
        );

        return new OrderAndPaymentData(order, orderDetails, payment);
    }

    /**
     * 주문 생성 및 결제
     */
    public OrderResponse completeOrder(Long userId, OrderCreateRequest request) {

        // 1. 멱등성 키 검사
        paymentService.checkForIdempotencyKey(request.idempotencyKey());
        
        OrderAndPaymentData initialData;
        try {
            // 2. 초기화
            initialData = initOrder(userId, request);
        } catch (BusinessException e) {
            throw e;
        }
        
        Payment payment = initialData.payment();
        Order order = initialData.order();
        List<OrderDetail> orderDetails = initialData.orderDetails();

        // 3. 외부 결제 처리 (시뮬레이션)
        String transactionId = "";
        if(payment.paymentType() == PaymentType.CARD){
            transactionId = processExternalPayment(payment, request);
        }
        
        // 7. 결제 완료 처리
        Payment completedPayment = paymentService.completePayment(payment.id(), transactionId);
        
        // 8. 주문 완료 처리
        Order completedOrder = orderService.completeOrder(order.id());
        
        // 9. 판매량 증가
        updateProductSales(orderDetails);
        
        log.info("주문 완료: orderId={}, paymentId={}, userId={}", 
            completedOrder.id(), completedPayment.id(), userId);
        
        return OrderResponse.from(completedOrder, completedPayment, orderDetails);
    }

    /**
     * 재고 확인 및 차감
     */
    private List<OrderDetail> processStockDeduction(List<OrderCreateRequest.OrderItem> items) {
        return items.stream()
            .map(item -> {
                Product product = productService.getProduct(item.productId());
                
                // 재고 확인
                if (!product.canPurchase(item.quantity())) {
                    throw new BusinessException(
                        ErrorCode.ORDER_STOCK_INSUFFICIENT,
                        product.productName(),
                        product.stock()
                    );
                }
                
                // 재고 차감
                productService.decreaseStock(product.id(), item.quantity());
                
                return OrderDetail.create(
                    null,
                    product.id(),
                    item.quantity(),
                    product.price()
                );
            })
            .toList();
    }

    /**
     * 총 금액 계산
     */
    private Integer calculateTotalPrice(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
            .mapToInt(detail -> detail.unitPrice() * detail.quantity())
            .sum();
    }

    /**
     * 외부 결제 처리 (시뮬레이션)
     */
    private String processExternalPayment(Payment payment, OrderCreateRequest request) {
        // 실제로는 PG사 API 호출
        // 여기서는 시뮬레이션
        
        if ("FAIL".equals(request.paymentType())) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
        
        // 트랜잭션 ID 생성
        return "TXN_" + UUID.randomUUID().toString();
    }

    /**
     * 판매량 증가
     */
    private void updateProductSales(List<OrderDetail> orderDetails) {
        orderDetails.forEach(detail -> {
            Product product = productService.getProduct(detail.productId());
            productService.increaseSalesQuantity(product.id(), detail.quantity());
        });
    }

}
