package kr.hhplus.be.server.application.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.order.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;

    /**
     * 주문 상세 저장
     */
    @Transactional
    public List<OrderDetail> saveOrderDetails(List<OrderDetail> orderDetails) {

        List<OrderDetail> saved = orderDetailRepository.saveAll(orderDetails);

        log.info("주문 상세 저장 완료: {} 건", saved.size());

        return saved;
    }

    /**
     * 주문 상세 단건 저장
     */
    @Transactional
    public OrderDetail saveOrderDetail(OrderDetail orderDetail) {
        OrderDetail saved = orderDetailRepository.save(orderDetail);
        log.info("주문 상세 저장: orderDetailId={}", saved.id());
        return saved;
    }

    /**
     * 주문 ID로 주문 상세 목록 조회
     */
    public List<OrderDetail> getOrderDetails(Long orderId) {
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(orderId);
        log.info("주문 상세 조회: orderId={}, count={}", orderId, orderDetails.size());
        return orderDetails;
    }

}
