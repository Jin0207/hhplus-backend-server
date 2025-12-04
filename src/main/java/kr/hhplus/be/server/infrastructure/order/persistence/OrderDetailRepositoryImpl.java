package kr.hhplus.be.server.infrastructure.order.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderDetailRepositoryImpl implements OrderDetailRepository {

    private final OrderDetailJpaRepository jpaRepository;

    @Override
    public OrderDetail save(OrderDetail orderDetail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OrderDetail> saveAll(List<OrderDetail> orderDetails) {
        // TODO Auto-generated method stub
        return null;
    }

}
