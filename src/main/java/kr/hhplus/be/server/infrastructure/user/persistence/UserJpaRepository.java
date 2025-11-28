package kr.hhplus.be.server.infrastructure.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  UserJpaRepository extends JpaRepository<UserEntity, Long>{
}
