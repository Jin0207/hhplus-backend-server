package kr.hhplus.be.server.domain.product.enums;

public enum ProductCategory{
    SHOESE("신발"),
    OUTER("아우터"),
    TOP("상의"),
    PANTS("하의");

    private final String description;

    ProductCategory(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}