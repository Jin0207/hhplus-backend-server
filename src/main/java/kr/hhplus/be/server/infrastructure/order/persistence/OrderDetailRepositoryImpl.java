package kr.hhplus.be.server.infrastructure.order.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderDetailRepositoryImpl implements OrderDetailRepository {

    private final OrderDetailJpaRepository orderDetailJpaRepository;

    @Override
    public List<OrderDetail> findByOrderId(Long orderId) {
        return orderDetailJpaRepository.findByOrderId(orderId).stream()
                .map(OrderDetailEntity::toDomain)
                .toList();
    }

    @Override
    public OrderDetail save(OrderDetail orderDetail) {
        OrderDetailEntity entity = OrderDetailEntity.from(orderDetail);
        return orderDetailJpaRepository.save(entity).toDomain();
    }

    @Override
    public List<OrderDetail> saveAll(List<OrderDetail> orderDetails) {
        // 1. Domain 리스트를 Entity 리스트로 변환
        List<OrderDetailEntity> entities = orderDetails.stream()
                .map(OrderDetailEntity::from)
                .toList();
        
        // 2. 일괄 저장 후 다시 Domain 리스트로 변환하여 반환
        return orderDetailJpaRepository.saveAll(entities).stream()
                .map(OrderDetailEntity::toDomain)
                .toList();
    }

}
