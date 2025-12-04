package kr.hhplus.be.server.infrastructure.order.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailJpaRepository extends JpaRepository<OrderDetailEntity, Long>{
    
}
