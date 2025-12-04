package kr.hhplus.be.server.infrastructure.order.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Optional<Order> findById(Long id) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public Order save(Order order) {
        // TODO Auto-generated method stub
        return null;
    }


}
