package kr.hhplus.be.server.application.point.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.enums.PointType;
import kr.hhplus.be.server.domain.point.repository.PointHistoryRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {
private final UserService userService;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 충전
     */
    @Transactional
    public User chargePoint(Long userId, Long amount, String comment) {
        // 1. 사용자 조회
        User user = userService.getUser(userId);
        
        // 2. 포인트 충전
        User chargedUser = user.chargePoint(amount);
        
        // 3. 이력 기록
        recordPointHistory(userId, amount, PointType.CHARGE, comment);
        
        // 4. 저장
        return userService.save(chargedUser);
    }

    /**
     * 포인트 사용
     * - 주문 프로세스에서만 호출됨으로 @Transactional 제거
     */
    public User usePoint(Long userId, Long amount, String comment) {
        User user = userService.getUser(userId);
        User usedUser = user.usePoint(amount);
        
        recordPointHistory(userId, amount, PointType.USE, comment);
        
        return userService.save(usedUser);
    }

    /**
     * 포인트 환불 (comment 없음)
     * - 주문 프로세스에서만 호출됨으로 @Transactional 제거
     */
    public User refundPoint(Long userId, Long amount) {
        return refundPoint(userId, amount, "환불");
    }

    /**
     * 포인트 환불 (comment 포함)
     * - 주문 취소 시 사용
     */
    public User refundPoint(Long userId, Long amount, String comment) {
        User user = userService.getUser(userId);
        User refundedUser = user.chargePoint(amount);

        recordPointHistory(userId, amount, PointType.CHARGE, comment);

        return userService.save(refundedUser);
    }

    /**
     * 이력 조회
     */
    @Transactional(readOnly = true)
    public List<PointHistory> getPointHistory(Long userId) {
        // 사용자 존재 확인
        userService.getUser(userId);
        return pointHistoryRepository.findByUserId(userId);
    }

    /**
     * 포인트 잔액 조회
     */
    @Transactional(readOnly = true)
    public Long getPointBalance(Long userId) {
        User user = userService.getUser(userId);
        return user.point();
    }

    /**
     * 이력 기록
     */
    private void recordPointHistory(Long userId, Long amount, PointType type, String comment) {
        PointHistory history = PointHistory.insert(userId, amount, type, comment);
        pointHistoryRepository.save(history);
    }
}
