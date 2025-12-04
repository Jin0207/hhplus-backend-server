package kr.hhplus.be.server.domain.product.enums;

public enum StockType {
    IN("입고"),   
    OUT("출고");

    private final String description;

    StockType(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}