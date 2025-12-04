package kr.hhplus.be.server.application.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.point.entity.PointHistory;
import kr.hhplus.be.server.domain.point.enums.PointType;
import kr.hhplus.be.server.domain.point.repository.PointHistoryRepository;
import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    /*
    * 사용자 조회
    */
    public User getUser(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));
        log.debug("유저 정보 - {}", user);
        return user;
    }

    /**
     * 포인트 충전
     */
    @Transactional
    public User chargePoint(Long userId, Integer amount, String comment) {

        if (amount <= 0) {
            throw new BusinessException(ErrorCode.CHARGE_LESS_THAN_ZERO);
        }
        // 1. 사용자 조회
        User user = getUser(userId);
        // 2. 포인트 충전
        User chargedUser = user.chargePoint(amount);
        
        // 3. 충전 이력 저장
        PointHistory history = PointHistory.insert(userId, amount, PointType.CHARGE, comment);
        pointHistoryRepository.save(history);
        
        // 4. 사용자 정보 저장
        return userRepository.save(chargedUser);
    }

    /**
     * 포인트 사용
     */
    @Transactional
    public User usePoint(Long userId, Integer amount, String comment) {
        // 1. 사용자 조회
        User user = getUser(userId);
        
        // 2. 포인트 사용
        User usedUser = user.usePoint(amount);
        
        // 3. 사용 이력 저장
        PointHistory history = PointHistory.insert(userId, amount, PointType.USE, comment);
        pointHistoryRepository.save(history);
        
        // 4. 사용자 정보 저장
        return userRepository.save(usedUser);
    }

    /**
     * 포인트 잔액 조회
     */
    public Integer getPointBalance(Long userId) {
        User user = getUser(userId);
        return user.point();
    }

    /**
     * 포인트 이력 조회
     */
    public List<PointHistory> getPointHistory(Long userId) {
        return pointHistoryRepository.findByUserId(userId);
    }
}