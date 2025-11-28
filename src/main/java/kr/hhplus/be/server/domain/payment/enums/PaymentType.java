package kr.hhplus.be.server.domain.payment.enums;

public enum PaymentType {
    CARD("카드"),
    BANK_TRANSFER("계좌이체"),
    POINT("포인트")
    ;

    private final String description;

    PaymentType(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
