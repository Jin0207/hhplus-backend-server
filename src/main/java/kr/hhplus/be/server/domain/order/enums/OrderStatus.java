package kr.hhplus.be.server.domain.order.enums;

public enum OrderStatus {
    PENDING("대기"), 
    COMPLETED("완료"),
    CANCELED("취소");

    private final String description;

    OrderStatus(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
