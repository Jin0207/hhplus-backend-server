package kr.hhplus.be.server.application.user.facade;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.point.response.PointHistoryResponse;
import kr.hhplus.be.server.application.user.service.UserService;
import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.presentation.user.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    /**
     * 포인트 충전
     */
    @Transactional
    public UserResponse chargePoint(Long userId, Integer amount, String comment) {
        User user = userService.chargePoint(userId, amount, comment);
        return UserResponse.from(user);
    }

    /**
     * 포인트 사용
     */
    @Transactional
    public UserResponse usePoint(Long userId, Integer amount) {
        User user = userService.usePoint(userId, amount, null);
        return UserResponse.from(user);
    }

    /**
     * 포인트 잔액 조회
     */
    @Transactional(readOnly = true)
    public Integer getPointBalance(Long userId) {
        return userService.getPointBalance(userId);
    }

    /**
     * 포인트 이력 조회
     */
    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getPointHistory(Long userId) {
        List<PointHistory> histories = userService.getPointHistory(userId);
        return histories.stream()
            .map(PointHistoryResponse::from)
            .toList();
    }
}
