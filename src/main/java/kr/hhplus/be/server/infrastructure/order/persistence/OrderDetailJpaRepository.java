package kr.hhplus.be.server.infrastructure.order.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailJpaRepository extends JpaRepository<OrderDetailEntity, Long>{
    List<OrderDetailEntity> findByOrderId(Long orderId);
}
