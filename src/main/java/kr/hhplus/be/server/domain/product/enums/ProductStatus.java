package kr.hhplus.be.server.domain.product.enums;

public enum ProductStatus {
    ON_SALE("판매중"),
    SOLD_OUT("품절"),
    INACTIVE("판매종료");

    private final String description;

    ProductStatus(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
