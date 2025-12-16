package kr.hhplus.be.server.domain.order.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.enums.OrderStatus;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByUserId(Long userId);
    List<Order> findByOrderStatus(OrderStatus status);
}
