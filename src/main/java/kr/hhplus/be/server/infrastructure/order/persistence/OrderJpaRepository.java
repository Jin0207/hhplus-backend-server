package kr.hhplus.be.server.infrastructure.order.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.hhplus.be.server.domain.order.enums.OrderStatus;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>{
    List<OrderEntity> findByUserId(Long userId);
    List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);
}
