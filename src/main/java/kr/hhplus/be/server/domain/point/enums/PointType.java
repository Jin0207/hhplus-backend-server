package kr.hhplus.be.server.domain.point.enums;

public enum PointType {
    USE("사용"), 
    CHARGE("충전");

    private final String description;

    PointType(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

}
