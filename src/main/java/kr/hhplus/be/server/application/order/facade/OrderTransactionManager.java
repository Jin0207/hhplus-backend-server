package kr.hhplus.be.server.application.order.facade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.application.order.service.OrderDetailService;
import kr.hhplus.be.server.application.order.service.OrderService;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.user.service.UserService;
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
    private final UserService userService;
    private final CouponService couponService;
    private final StockManagerImpl stockManager;

    /**
     * 주문 초기화 (재고 차감, 포인트 차감)
     */
    @Transactional
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
            userService.usePoint(userId, orderPrice.finalPrice(), "주문 결제");
            
            // 5. 주문 생성
            Order order = orderService.createOrder(userId, orderPrice);

            // 6. 주문 상세 ID 매핑 및 저장
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
                request.paymentType()  // 포인트 결제
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
     * 주문 롤백
     * - 재고 복구 + Stock 이력 기록
     * - 포인트 환불
     */
    @Transactional
    public void rollbackOrder(OrderAndPayment orderData) {
        log.info("주문 롤백 시작: orderId={}", orderData.order().id());
        
        try {
            Long orderId = orderData.order().id();
            Order order = orderData.order();
            
            // 1. 주문 취소
            orderService.cancelOrder(orderId);
            
            // 2. 결제 실패 처리
            paymentService.failPayment(
                orderData.payment().id(), 
                "결제 처리 실패"
            );
            
            // 3. 재고 복구
            stockManager.restoreStock(orderData.orderDetails());
            
            // 4. 쿠폰 복구 (사용했다면)
            if (order.couponId() != null) {
                couponService.restoreCoupon(order.userId(), order.couponId());
                log.info("쿠폰 복구 완료: userId={}, couponId={}", 
                    order.userId(), order.couponId());
            }
            
            // 5. 포인트 환불
            Integer paidAmount = orderData.payment().price();
            userService.chargePoint(
                order.userId(), 
                paidAmount, 
                "주문 취소 - 포인트 환불 (orderId: " + orderId + ")"
            );
            
            log.info("주문 롤백 완료: orderId={}", orderId);
            
        } catch (Exception e) {
            log.error("주문 롤백 실패: orderId={}", orderData.order().id(), e);
            throw new BusinessException(ErrorCode.ORDER_FAILED, "롤백");
        }
    }
}
