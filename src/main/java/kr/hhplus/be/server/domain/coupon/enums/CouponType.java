package kr.hhplus.be.server.domain.coupon.enums;

public enum CouponType {
    AMOUNT("금액"), 
    PERCENT("퍼센트");

    private final String description;

    CouponType(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
