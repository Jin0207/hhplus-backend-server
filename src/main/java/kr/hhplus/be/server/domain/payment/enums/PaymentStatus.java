package kr.hhplus.be.server.domain.payment.enums;

public enum PaymentStatus {
    PENDING("대기"), 
    COMPLETED("완료"),
    CANCELED("취소"),
    FAILED("실패")
    ;

    private final String description;

    PaymentStatus(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}   
