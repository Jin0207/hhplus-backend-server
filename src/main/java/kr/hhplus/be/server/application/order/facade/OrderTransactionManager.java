package kr.hhplus.be.server.application.order.facade;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.service.OrderDetailService;
import kr.hhplus.be.server.application.order.service.OrderService;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.application.product.facade.StockManagerImpl;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 트랜잭션 관리자
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionManager {
    private final OrderService orderService;
    private final OrderDetailService orderDetailService; 
    private final PaymentService paymentService;
    private final OrderPriceCalculator priceCalculator;
    private final PointService pointService;
    private final CouponService couponService;
    private final StockManagerImpl stockManager;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 초기화 (재고 차감, 포인트 차감)
     */
    public OrderAndPayment initializeOrder(Long userId, OrderCreateRequest request) {
        log.info("주문 초기화 시작: userId={}", userId);
        
        try {
            /**
             *  1. 재고 예약 및 OrderDetail 준비
             *  - 상품 재고차감 수행(누적판매량 증가x)
             */ 
            List<OrderDetail> orderDetails = stockManager.reserveStock(request.items());          
            // 2. 가격 계산
            OrderPrice orderPrice = priceCalculator.calculate(userId, orderDetails, request);
            
            // 3. 쿠폰 사용 처리 (사용 가능 여부 검증 포함)
            if (orderPrice.couponId() != null) {
                couponService.useCoupon(userId, orderPrice.couponId());
            }

            // 4. 포인트 차감
            pointService.usePoint(userId, orderPrice.finalPrice(), "주문 결제");
            
            // 5. 주문 생성
            Order order = orderService.createOrder(userId, orderPrice);

            // 6. 주문 상세 저장
            List<OrderDetail> detailsWithOrderId = orderDetails.stream()
                .map(detail -> detail.assignOrderId(order.id()))
                .toList();

            orderDetailService.saveOrderDetails(detailsWithOrderId);

            stockManager.recordStockOut(order.id(), orderDetails, "상품 주문");

            // 7. 결제 레코드 생성 (포인트 결제)
            Payment payment = paymentService.createPayment(
                order.id(),
                userId,
                request.idempotencyKey(),
                orderPrice.finalPrice(),
                request.paymentType()
            );
            
            log.info("주문 초기화 완료: orderId={}, paymentId={}", order.id(), payment.id());
            
            return OrderAndPayment.builder()
                .order(order)
                .orderDetails(orderDetails)
                .payment(payment)
                .build();
                
        } catch (Exception e) {
            log.error("주문 초기화 실패: userId={}", userId, e);
            throw e;
        }
    }

    /**
     *  주문 완료 처리
     */
    public OrderResponse completeOrder(
            OrderAndPayment orderData, 
            PaymentResult paymentResult) {
        
        try {
            // 1. 결제 완료
            Payment completedPayment = paymentService.completePayment(
                orderData.payment().id(), 
                paymentResult.transactionId()
            );
            
            // 2. 주문 완료
            Order completedOrder = orderService.completeOrder(orderData.order().id());
            
            // 3. 판매량 증가
            updateProductSales(orderData.orderDetails());
            
            // 4. 이벤트 발행 (트랜잭션 커밋 후)
            eventPublisher.publishEvent(orderData);

            log.info("주문 완료: orderId={}, paymentId={}", 
                completedOrder.id(), completedPayment.id());
            
            return OrderResponse.from(
                completedOrder, 
                completedPayment, 
                orderData.orderDetails()
            );
            
        } catch (Exception e) {
            log.error("주문 완료 처리 실패", e);
            throw new BusinessException(ErrorCode.ORDER_FAILED, "완료처리");
        }
    }
    
    private void updateProductSales(List<OrderDetail> orderDetails) {
        orderDetails.forEach(detail -> {
            productService.increaseSalesQuantity(
                detail.productId(), 
                detail.quantity()
            );
        });
    }
}
