package kr.hhplus.be.server.application.order.facade;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.application.product.service.StockService;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;


/**
 * - Product 재고 차감/복구
 * - Stock 이력 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockManager {
    private final ProductService productService;
    private final StockService stockService;

    /**
     * 재고 차감
     */
    @Transactional
    public List<OrderDetail> decreaseStock(List<OrderCreateRequest.OrderItem> items, Long orderId){
        return items.stream().map(item -> decreaseStockForItem(item, orderId))
            .toList();
    }

    private OrderDetail decreaseStockForItem(OrderCreateRequest.OrderItem item, Long orderId) {
        Product product = productService.getProduct(item.productId());
        
        // 1. 재고 확인
        if (!product.canPurchase(item.quantity())) {
            throw new BusinessException(
                ErrorCode.ORDER_STOCK_INSUFFICIENT,
                product.productName(),
                product.stock()
            );
        }
        
        // 2. Product 테이블 재고 차감
        productService.decreaseStock(product.id(), item.quantity());
        
        // 3. Stock 테이블에 출고 이력 기록
        stockService.recordOut(
            product.id(), 
            item.quantity(), 
            "주문 출고 (orderId: " + orderId + ")"
        );
        
        log.debug("재고 차감 완료: productId={}, quantity={}", 
            product.id(), item.quantity());
        
        return OrderDetail.create(
            null,
            product.id(),
            item.quantity(),
            product.price()
        );
    }
    
    /**
     * 재고 복구 (주문 취소 시)
     */
    @Transactional
    public void restoreStock(List<OrderDetail> orderDetails, Long orderId) {
        log.info("재고 복구 시작: {} 건, orderId={}", orderDetails.size(), orderId);
        
        orderDetails.forEach(detail -> {
            // 1. Product 테이블 재고 복구
            productService.increaseStock(detail.productId(), detail.quantity());
            
            // 2. Stock 테이블에 입고 이력 기록
            stockService.recordIn(
                detail.productId(), 
                detail.quantity(), 
                "주문 취소 입고 (orderId: " + orderId + ")"
            );
            
            log.debug("재고 복구 완료: productId={}, quantity={}", 
                detail.productId(), detail.quantity());
        });
    }
}
