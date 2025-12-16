package kr.hhplus.be.server.application.order.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.facade.interfaces.StockManager;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.application.product.service.StockService;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * 재고 관리자
 * - Product.stock으로 재고 확인 및 차감/복구만 담당
 * - Stock 테이블 이력은 OrderService에서 별도로 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockManagerImpl implements StockManager{
    private final ProductService productService;
    private final StockService stockService;
    
    /**
     * 재고 예약 (차감)
     */
    @Override
    public List<OrderDetail> reserveStock(List<OrderCreateRequest.OrderItem> items) {
        return items.stream()
            .map(this::reserveStockForItem)
            .toList();
    }
    
    // 검증 후 재고를 차감한다.
    private OrderDetail reserveStockForItem(OrderCreateRequest.OrderItem item) {
        Product updatedProduct = productService.decreaseStock(item.productId(), item.quantity());
        
        log.debug("재고 차감 완료: productId={}, quantity={}, remainStock={}", 
            updatedProduct.id(), item.quantity(), updatedProduct.stock());
        
        return OrderDetail.create(
            null,
            updatedProduct.id(),
            item.quantity(),
            updatedProduct.price()
        );
    }
    
    /**
     * 재고 차감 이력 기록
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStockOut(Long orderId, List<OrderDetail> orderDetails, String reason) {
        log.info("재고 차감 이력 기록: orderId={}", orderId);
        stockService.recordOut(orderId, orderDetails, reason);
    }

    /**
     * 재고 복구 (주문 취소 시)
     */
    public void restoreStock(List<OrderDetail> orderDetails) {
        log.info("재고 복구 시작: {} 건", orderDetails.size());
        
        orderDetails.forEach(detail -> {
            // Product.stock 복구
            productService.increaseStock(detail.productId(), detail.quantity());
            
            log.debug("재고 복구 완료: productId={}, quantity={}", 
                detail.productId(), detail.quantity());
        });
    }
}