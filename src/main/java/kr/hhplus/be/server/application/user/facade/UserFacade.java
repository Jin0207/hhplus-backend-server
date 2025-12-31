package kr.hhplus.be.server.application.user.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import kr.hhplus.be.server.application.point.response.PointHistoryResponse;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.presentation.user.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFacade {
    private final PointService pointService;

    /**
     * 포인트 충전
     */
    public UserResponse chargePoint(Long userId, Long amount, String comment) {
        User user = pointService.chargePoint(userId, amount, comment);
        return UserResponse.from(user);
    }

    /**
     * 포인트 사용
     */
    public UserResponse usePoint(Long userId, Long amount) {
        User user = pointService.usePoint(userId, amount, null);
        return UserResponse.from(user);
    }

    /**
     * 포인트 잔액 조회
     */
    public Long getPointBalance(Long userId) {
        return pointService.getPointBalance(userId);
    }

    /**
     * 포인트 이력 조회
     */
    public List<PointHistoryResponse> getPointHistory(Long userId) {
        List<PointHistory> histories = pointService.getPointHistory(userId);
        return histories.stream()
            .map(PointHistoryResponse::from)
            .toList();
    }
}
