package kr.hhplus.be.server.application.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    /**
     * 주문 생성
     */
    @Transactional
    public Order createOrder(
            Long userId,
            Long couponId,
            Integer totalPrice,
            Integer discountPrice,
            Integer finalPrice,
            List<OrderDetail> orderDetails) {
        
        // 1. 주문 생성
        Order order = Order.create(userId, couponId, totalPrice, discountPrice, finalPrice);
        Order savedOrder = orderRepository.save(order);
        
        // 2. 주문 상세 저장
        List<OrderDetail> detailsWithOrderId = orderDetails.stream()
            .map(detail -> OrderDetail.create(
                savedOrder.id(),
                detail.productId(),
                detail.quantity(),
                detail.unitPrice()
            ))
            .toList();
        
        orderDetailRepository.saveAll(detailsWithOrderId);
        
        return savedOrder;
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
        Order order = getOrder(orderId);
        Order completedOrder = order.complete();
        return orderRepository.save(completedOrder);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrder(orderId);
        Order canceledOrder = order.cancel();
        return orderRepository.save(canceledOrder);
    }
}
