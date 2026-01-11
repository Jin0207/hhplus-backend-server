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
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    
    /*
    * 사용자 저장
    */
    public User save(User chargedUser){
        return userRepository.save(chargedUser);
    }

    /*
    * 사용자 생성
    */
    @Transactional
    public User createUser(User user){
        // 중복 계정 확인
        if (userRepository.existsByAccountId(user.accountId())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, user.accountId());
        }

        User savedUser = userRepository.save(user);
        log.info("사용자 생성 완료: userId={}, accountId={}", savedUser.id(), savedUser.accountId());
        
        return savedUser;
    }

    /*
    * 사용자 조회 (ID)
    */
    public User getUser(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, userId));
        log.debug("유저 정보 - {}", user);
        return user;
    }

    /**
     * 사용자 조회 (AccountId)
     */
    public User getUserByAccountId(String accountId) {
        User user = userRepository.findByAccountId(accountId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, accountId));
        log.debug("유저 정보 - {}", user);
        return user;
    }
}