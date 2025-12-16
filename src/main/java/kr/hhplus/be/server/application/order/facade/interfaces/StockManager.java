package kr.hhplus.be.server.application.order.facade.interfaces;

import java.util.List;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;

public interface StockManager {
    /**
     * 재고 확인 및 차감 (재고 예약). 재고 부족 시 BusinessException 발생.
     * @return OrderDetail 리스트 (단가, 수량 포함)
     */
    List<OrderDetail> reserveStock(List<OrderCreateRequest.OrderItem> items);

    // 2. 재고 차감 이력 기록
    void recordStockOut(Long orderId, List<OrderDetail> orderDetails, String reason); // 🌟 추가

    /**
     * 재고 복구 (주문 취소 시)
     */
    void restoreStock(List<OrderDetail> orderDetails);
    
}