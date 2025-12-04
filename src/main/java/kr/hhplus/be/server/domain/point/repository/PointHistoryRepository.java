package kr.hhplus.be.server.domain.point.repository;


import java.util.List;

import kr.hhplus.be.server.domain.point.entity.PointHistory;

public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findByUserId(Long userId);
}
