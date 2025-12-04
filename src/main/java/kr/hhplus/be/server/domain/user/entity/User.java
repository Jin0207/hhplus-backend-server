package kr.hhplus.be.server.domain.user.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

// ============================================
// 사용자 및 보유포인트 관리
// ============================================
public record User(
    Long id,             // 식별자
    String accountId,       // 유저ID
    String password,        // PWD
    Integer point,          // 보유포인트
    LocalDateTime crtDttm,  // 생성일
    LocalDateTime updDttm   // 수정일
) {
    /**
     * 새로운 사용자 생성
     */
    public static User create(String accountId, String password) {
        validateAccountId(accountId);
        validatePassword(password);
        
        LocalDateTime now = LocalDateTime.now();
        return new User(null, accountId, password, 0, now, null);
    }

    /*
     * 포인트를 충전한다.
     */
    public User chargePoint(Integer amount) {
        validatePointAmount(amount);

        //최대보유포인트 '1백만'포인트
        if(point+amount > 1_000_000){
            throw new BusinessException(ErrorCode.POINT_BALANCE_MAX);
        }

        LocalDateTime now = LocalDateTime.now();
        return new User(id, accountId, password, point + amount, crtDttm, now);
    }
    
    /*
     * 포인트를 사용한다.
     */
    public User usePoint(Integer amount) {
        validatePointAmount(amount);

        if (this.point < amount) {
            throw new BusinessException(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new User(id, accountId, password, point - amount, crtDttm, now);
    }
    /**
     * 포인트 충분 여부 확인
     */
    public boolean hasEnoughPoint(Integer amount) {
        return this.point >= amount;
    }

    // ============================================
    // 검증 메서드
    // ============================================
    
    /* 
    * 포인트 금액을 검증한다.
    */
    private static void validatePointAmount(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.CHARGE_LESS_THAN_ZERO);
        }
    }
    /*
    * 유저 ID 검증한다.
    */
    private static void validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "계정ID");
        }
        if (accountId.length() > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,"계정ID", "50자를 초과");
        }
    }
    /*
    * Password 검증한다.
    */
    private static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "비밀번호");
        }
    }

}