package kr.hhplus.be.server.domain.coupon.enums;

public enum CouponStatus {
    ACTIVE("활성화"), 
    INACTIVE("비활성화");

    private final String description;

    CouponStatus(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
