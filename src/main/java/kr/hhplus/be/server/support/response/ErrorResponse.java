package kr.hhplus.be.server.support.response;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String code;
    private final String message;
    private final Object details;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.details = null;
    }

    public ErrorResponse(String code, String message, Object details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}

