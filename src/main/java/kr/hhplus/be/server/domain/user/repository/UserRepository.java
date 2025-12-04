package kr.hhplus.be.server.domain.user.repository;

import java.util.Optional;

import kr.hhplus.be.server.domain.user.entity.User;

public interface UserRepository {
    /**
     * userId로 사용자 조회
     */
    Optional<User> findById(Long userId);
    /**
     * accountId로 사용자 조회
     */
    Optional<User> findByAccountId(String accountId);
    /**
     * accountId 중복 체크
     */
    boolean existsByAccountId(String accountId);
    /**
     * 저장
     */
    User save(User user);
}