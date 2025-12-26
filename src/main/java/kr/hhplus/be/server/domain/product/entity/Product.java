package kr.hhplus.be.server.domain.product.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
// ============================================
// 상품 관리
// ============================================
public record Product(
    Long id,                 // 식별자
    String productName,         // 상품명
    Long price,              // 가격
    Integer stock,              // 재고
    ProductCategory category,   // 카테고리
    ProductStatus status,       // 상태
    Integer salesQuantity,      // 누적판매량
    LocalDateTime crtDttm,      // 생성일
    LocalDateTime updDttm       // 수정일
) {  
    /**
     * 재고  증가
     * @param quantity 증가할 수량
     * @return Product
     */
    public Product increaseStock(Integer quantity) {
        validateQuantity(quantity);
        
        Integer newStock = this.stock + quantity;
        ProductStatus newStatus = (this.status == ProductStatus.SOLD_OUT) ? ProductStatus.ON_SALE : this.status;
        
        return new Product(
            this.id,
            this.productName,
            this.price,
            newStock,
            this.category,
            newStatus,
            this.salesQuantity,
            this.crtDttm,
            LocalDateTime.now()
        );
    }

    /**
     * 재고 차감
     * @param quantity 차감할 수량
     * @return Product
     */
    public Product decreaseStock(Integer quantity) {
        validateQuantity(quantity);
        
        if (this.stock < quantity) {
            throw new BusinessException(
                ErrorCode.ORDER_STOCK_INSUFFICIENT,
                this.productName,
                this.stock
            );
        }
        
        Integer newStock = this.stock - quantity;
        ProductStatus newStatus = (newStock == 0) ? ProductStatus.SOLD_OUT : this.status;
        
        return new Product(
            this.id,
            this.productName,
            this.price,
            newStock,
            this.category,
            newStatus,
            this.salesQuantity,
            this.crtDttm,
            LocalDateTime.now()
        );
    }

    /**
     * 판매량 증가 (주문 완료 시)
     * @param quantity 판매된 수량
     * @return 판매량이 증가된 새로운 Product
     */
    public Product increaseSalesQuantity(Integer quantity) {
        validateQuantity(quantity);
        
        Integer newSalesQuantity = this.salesQuantity + quantity;
        
        return new Product(
            this.id,
            this.productName,
            this.price,
            this.stock,
            this.category,
            this.status,
            newSalesQuantity,
            this.crtDttm,
            LocalDateTime.now()
        );
    }

    // ==================== 주문 시 검증 메서드 ====================
    
    /**
     * 구매 가능 여부 확인
     * @param quantity 구매하려는 수량
     * @return 구매 가능하면 true
     */
    public boolean canPurchase(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        
        // '판매중'인 경우 구매가능
        return this.status == ProductStatus.ON_SALE 
            && this.stock >= quantity;
    }

    /**
     * 판매 가능 상태 확인
     * @return 판매 가능하면 true
     */
    public boolean isAvailableForSale() {
        return this.status == ProductStatus.ON_SALE && this.stock > 0;
    }

    /**
     * 품절 상태 확인
     * @return 품절이면 true
     */
    public boolean isSoldOut() {
        return this.status == ProductStatus.SOLD_OUT || this.stock == 0;
    }

    /**
     * 재고 부족 여부 확인 (10개 이하)
     * @return 재고 부족이면 true
     */
    public boolean isLowStock() {
        return this.stock > 0 && this.stock <= 10;
    }

    /**
     * 주문 가능 최대 수량 반환
     * @return 현재 주문 가능한 최대 수량
     */
    public Integer getAvailableQuantity() {
        if (this.status != ProductStatus.ON_SALE) {
            return 0;
        }
        return this.stock;
    }

    /**
     * 총 주문 금액 계산
     * @param quantity 주문 수량
     * @return 총 금액
     */
    public Long calculateTotalPrice(Integer quantity) {
        validateQuantity(quantity);
        return this.price * quantity;
    }

    // ==================== 검증 메서드 ====================
    
    /**
     * 상품 판매 가능 상태 검증 (주문 전)
     */
    public void validateForOrder(Integer quantity) {
        // 판매 상태 확인
        if (this.status != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_INACTIVE);
        }
        
        // 재고 확인
        if (this.stock < quantity) {
            throw new BusinessException(
                ErrorCode.ORDER_STOCK_INSUFFICIENT,
                this.productName,
                this.stock
            );
        }
        
        // 수량 검증
        validateQuantity(quantity);
    }

    /**
     * 수량 검증
     */
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            // 수량은 0보다 작을 수 없습니다.
            throw new BusinessException(ErrorCode.LESS_THAN_ZERO, "수량");
        }
    }
}