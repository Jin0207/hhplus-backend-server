package kr.hhplus.be.server.application.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    /**
     * 출고 이력 기록
     */
    public void recordOut(Long orderId, List<OrderDetail> orderDetails, String reason){
        log.info("주문 출고 이력 기록: orderId={}, items={}", orderId, orderDetails.size());
        
        orderDetails.forEach(detail -> {
            Stock stock = Stock.createOut(
                detail.productId(),
                detail.quantity(),
                reason
            );
            stockRepository.save(stock);
            
            log.debug("출고 이력: productId={}, quantity={}", 
                detail.productId(), detail.quantity());
        });
        
        log.info("주문 출고 이력 기록 완료: {} 건", orderDetails.size());
    }
    /**
     * 입고 이력 기록 (재고 복구 시)
     */
    public void recordIn(Long orderId, List<OrderDetail> orderDetails, String reason){
        log.info("주문 취소 입고 이력 기록: orderId={}, items={}", orderId, orderDetails.size());
        
        orderDetails.forEach(detail -> {
            Stock stock = Stock.createIn(
                detail.productId(),
                detail.quantity(),
                reason
            );
            stockRepository.save(stock);
            
            log.debug("입고 이력: productId={}, quantity={}", 
                detail.productId(), detail.quantity());
        });
        
        log.info("주문 취소 입고 이력 기록 완료: {} 건", orderDetails.size());
    }
    
}
