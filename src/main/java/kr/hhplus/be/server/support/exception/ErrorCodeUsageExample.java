package kr.hhplus.be.server.support.exception;

/**
 * ErrorCode 사용 예시
 * 
 * 1. BusinessException과 함께 사용:
 *    throw new BusinessException(ErrorCode.CHARGE_LESS_THAN_ZERO);
 * 
 * 2. ApiResponse에서 직접 사용:
 *    return ResponseEntity.ok(ApiResponse.error(ErrorCode.USER_NOT_FOUND));
 * 
 * 3. 상세 정보와 함께 사용:
 *    return ResponseEntity.ok(ApiResponse.error(ErrorCode.VALIDATION_ERROR, errors));
 */
public class ErrorCodeUsageExample {
    
    // 예시 1: Service 계층에서 사용
    public void chargePoint(Integer amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.CHARGE_LESS_THAN_ZERO);
        }
        // 비즈니스 로직...
    }
    
    // 예시 2: Controller에서 직접 사용 (비즈니스 로직이 간단한 경우)
    // public ResponseEntity<ApiResponse<Object>> getPoint(Long userId) {
    //     if (user == null) {
    //         return ResponseEntity
    //                 .status(ErrorCode.USER_NOT_FOUND.getHttpStatus())
    //                 .body(ApiResponse.error(ErrorCode.USER_NOT_FOUND));
    //     }
    //     // ...
    // }
}

