package kr.hhplus.be.server.support.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 공통 에러 (E000 ~ E099)
    VALIDATION_ERROR("E000", "%s는/은 필수 입력입니다.", HttpStatus.BAD_REQUEST),
    ILLEGAL_ARGUMENT("E001", "잘못된 인자가 전달되었습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("E002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    RUNTIME_ERROR("E003", "런타임 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    RESOURCE_ACTION_REASON("E004", "%s는/은 %s할수없습니다.", HttpStatus.BAD_REQUEST),
    LESS_THAN_ZERO("E005", "%s는/은 0보다 작을 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 포인트 관련 에러 (E100 ~ E199)
    CHARGE_LESS_THAN_ZERO("E100", "충전/사용 금액은 0보다 작을 수 없습니다.", HttpStatus.BAD_REQUEST),
    CHARGE_AMOUNT_INVALID("E101", "충전 금액이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    POINT_BALANCE_INSUFFICIENT("E102", "포인트 잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    POINT_BALANCE_MAX("E103", "보유 포인트가 100만을 초과할 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("E104", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 주문 관련 에러 (E200 ~ E299)
    ORDER_FAILED("E200", "주문 %s에 실패했습니다.", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND("E201", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORDER_ITEM_EMPTY("E202", "주문 상품은 최소 1개 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    ORDER_STOCK_INSUFFICIENT("E203", "재고가 부족합니다.(상품명: %s, 현재재고: %s)", HttpStatus.BAD_REQUEST),
    
    // 결제 관련 에러 (E300 ~ E399)
    PAYMENT_FAILED("E300", "결제 처리에 실패했습니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND("E301", "결제 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_PROCESSED("E302", "이미 처리된 결제입니다.", HttpStatus.BAD_REQUEST),
    
    // 쿠폰 관련 에러 (E400 ~ E499)
    COUPON_NOT_FOUND("E400", "쿠폰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COUPON_ALREADY_ISSUED("E401", "이미 발급된 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    COUPON_OUT_OF_STOCK("E402", "쿠폰이 품절되었습니다.", HttpStatus.BAD_REQUEST),
    COUPON_EXPIRED("E403", "만료된 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    COUPON_MIN_OREDER_PRICE_NOT_MET("E404", "최소 주문 금액을 충족하지 못했습니다.(최소주문금액: %s)", HttpStatus.BAD_REQUEST),
    COUPON_NOT_AVAILABLE("E405", "사용불가능한 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    COUPON_ISSUE_FAILED("E406", "쿠폰 발급에 실패하였습니다.", HttpStatus.BAD_REQUEST),
    
    // 상품 관련 에러 (E500 ~ E599)
    PRODUCT_NOT_FOUND("E500", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK("E501", "상품이 품절되었습니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_INACTIVE("E502", "판매 종료된 상품입니다.", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_RANGE_INVALID("E503", "가격 범위가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 동적 메시지 생성
     * @param args 메시지 포맷팅에 사용될 인자들
     * @return 포맷팅된 메시지
     */
    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}

