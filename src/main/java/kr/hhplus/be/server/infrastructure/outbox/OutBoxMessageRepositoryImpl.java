package kr.hhplus.be.server.infrastructure.outbox;

import org.springframework.stereotype.Repository;

import kr.hhplus.be.server.domain.outbox.repository.OutBoxMessageRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OutBoxMessageRepositoryImpl implements OutBoxMessageRepository{

}
