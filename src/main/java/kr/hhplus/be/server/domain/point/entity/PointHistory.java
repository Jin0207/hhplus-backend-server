package kr.hhplus.be.server.domain.point.entity;

import java.time.LocalDateTime;

import kr.hhplus.be.server.domain.point.enums.PointType;
// ============================================
// 포인트 사용/충전 내역
// ============================================
public record PointHistory(
    Long id,             // 식별자
    Long userId,         // 사용자 식별자
    Integer point,          // 포인트
    PointType type,         // 타입(충전/사용)
    String comment,         // 비고
    LocalDateTime crtDttm   // 생성일
) {
    /**
     * 포인트 충전/사용 이력 생성
     */
    public static PointHistory insert(Long userId, Integer amount, PointType pointType, String comment) {
        return new PointHistory(
            null,
            userId,
            amount,
            pointType,
            comment,
            LocalDateTime.now()
        );
    }

}