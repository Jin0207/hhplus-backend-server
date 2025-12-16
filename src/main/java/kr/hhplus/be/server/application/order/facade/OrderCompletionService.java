package kr.hhplus.be.server.application.order.facade;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.dto.response.PaymentResult;
import kr.hhplus.be.server.application.order.service.OrderService;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionService {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 주문 완료 처리 - 최종 트랜잭션
     */
    @Transactional
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