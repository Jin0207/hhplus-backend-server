package kr.hhplus.be.server.infrastructure.order.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>{
    
}
