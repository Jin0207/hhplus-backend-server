package kr.hhplus.be.server.domain.coupon.enums;

public enum UserCouponStatus {
    AVAILABLE("사용가능"), 
    USED("사용완료"), 
    EXPIRED("사용불가");

    private final String description;

    UserCouponStatus(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
