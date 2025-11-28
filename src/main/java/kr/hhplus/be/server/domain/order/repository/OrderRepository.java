package kr.hhplus.be.server.domain.order.repository;

import java.util.Optional;

import kr.hhplus.be.server.domain.order.entity.Order;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
}
