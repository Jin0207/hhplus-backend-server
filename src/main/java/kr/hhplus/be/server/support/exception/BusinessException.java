package kr.hhplus.be.server.support.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;

    // 1. 기본 생성자 (정적 메시지)
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
    }

    // 2. 동적 메시지 생성자 (권장)
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.formatMessage(args));
        this.errorCode = errorCode;
        this.args = args;
    }

    // 3. 커스텀 메시지 (ErrorCode 메시지 무시)
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.args = null;
    }

    // 4. 원인 예외 포함
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.formatMessage(args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}

