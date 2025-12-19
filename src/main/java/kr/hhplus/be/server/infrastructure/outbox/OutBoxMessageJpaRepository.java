package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutBoxMessageJpaRepository extends JpaRepository<OutBoxMessageEntity, Long>{

}
