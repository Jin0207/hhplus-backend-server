package kr.hhplus.be.server.infrastructure.user.persistence;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository{
    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
            .map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity;
        
        if (user.id() == null) {
            // 신규 생성
            entity = UserEntity.from(user);
        } else {
            // 업데이트
            entity = jpaRepository.findById(user.id())
                .orElse(UserEntity.from(user));
            entity.updateFromDomain(user);
        }
        
        UserEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    public Optional<User> findByAccountId(String accountId) {
        return jpaRepository.findByAccountId(accountId).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByAccountId(String accountId) {
        return jpaRepository.existsByAccountId(accountId);
    }
}
