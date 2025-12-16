package kr.hhplus.be.server.application.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.order.dto.response.OrderPrice;
import kr.hhplus.be.server.application.product.service.StockService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockService stockService;

    /**
     * 주문 생성
     */
    @Transactional
    public Order createOrder(Long userId, OrderPrice price) {
        log.info("주문 생성 시작: userId={}", userId);
        Order order = Order.create(userId, price.couponId(), price.totalPrice(), price.discountPrice(), price.finalPrice());
        return orderRepository.save(order);
    }

    /**
     * 주문 조회
     */
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, orderId));
    }

    /**
     * 주문 완료
     */
    @Transactional
    public Order completeOrder(Long orderId) {
        log.info("주문 완료 처리: orderId={}", orderId);

        Order order = getOrder(orderId);

        if (order.orderStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                ErrorCode.INVALID_VALUE, 
                "주문상태:" + order.orderStatus()
            );
        }

        Order completedOrder = order.complete();
        return orderRepository.save(completedOrder);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrder(orderId);

        if (!order.canCancel()) {
            throw new BusinessException(
                ErrorCode.INVALID_VALUE, 
                "주문상태:" + order.orderStatus()
            );
        }
        
        Order canceledOrder = order.cancel();
        Order saved = orderRepository.save(canceledOrder);

        return saved;
    }
}
