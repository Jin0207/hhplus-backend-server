package kr.hhplus.be.server.domain.order.repository;

import java.util.List;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;

public interface OrderDetailRepository {
    OrderDetail save(OrderDetail orderDetail);
    List<OrderDetail> saveAll(List<OrderDetail> orderDetails);
}
