package kr.hhplus.be.server.application.order.facade;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderAndPayment;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.payment.dto.response.PaymentResult;
import kr.hhplus.be.server.application.payment.facade.PaymentProcessorImpl;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderTransactionManager orderTransactionManager;
    private final PaymentProcessorImpl paymentProcessor;
    private final OrderCompletionService orderCompletionService;

    /**
     * 주문 및 결제 완료
     */
    public OrderResponse completeOrder(Long userId, OrderCreateRequest request) {

        try {
            // 1. 멱등성 검사
            paymentProcessor.validateIdempotencyKey(request.idempotencyKey());
            
            // 2. 주문 초기화 (재고 차감, 포인트 차감)
            OrderAndPayment initialData = orderTransactionManager.initializeOrder(userId, request);
            
            // 3. 포인트 결제 처리
            PaymentResult paymentResult = paymentProcessor.processPayment(
                initialData.payment(), 
                request
            );
            
            // 4. 주문 완료 처리
            if (paymentResult.isSuccess()) {
                OrderResponse response = orderCompletionService.completeOrder(
                    initialData, 
                    paymentResult
                );

                return response;
            } else {
                // 포인트 결제는 항상 성공하므로 여기 올 일 없음
                orderTransactionManager.rollbackOrder(initialData);
                throw new BusinessException(ErrorCode.PAYMENT_FAILED, paymentResult.failReason());
            }
        } catch (BusinessException e) {
            log.error("주문 처리 실패 (비즈니스): userId={}, error={}", 
                userId, e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("주문 처리 실패 (시스템): userId={}", userId, e);
            throw new BusinessException(ErrorCode.ORDER_FAILED, "처리");
        }
    }

}
