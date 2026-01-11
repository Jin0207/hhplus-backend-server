package kr.hhplus.be.server.infrastructure.user.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.hhplus.be.server.domain.user.entity.User;

public interface  UserJpaRepository extends JpaRepository<UserEntity, Long>{
    Optional<UserEntity> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
}
