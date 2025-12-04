package kr.hhplus.be.server.support.response;

import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, String message, T data, ErrorResponse error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, message, null, new ErrorResponse(code, message));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                errorCode.getMessage(),
                null,
                new ErrorResponse(errorCode.getCode(), errorCode.getMessage())
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, Object details) {
        return new ApiResponse<>(
                false,
                errorCode.getMessage(),
                null,
                new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), details)
        );
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, error.getMessage(), null, error);
    }
}

