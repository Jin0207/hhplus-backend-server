package kr.hhplus.be.server.application.order.dto.response;

import java.util.List;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;

public record OrderCompletedEvent(
    Order order,
    List<OrderDetail> orderDetails
) {}