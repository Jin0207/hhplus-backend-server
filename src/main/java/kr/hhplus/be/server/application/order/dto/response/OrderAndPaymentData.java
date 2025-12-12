package kr.hhplus.be.server.application.order.dto.response;

import java.util.List;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import lombok.Builder;

@Builder
public record OrderAndPaymentData (
    Order order,
    List<OrderDetail> orderDetails,
    Payment payment
){}
