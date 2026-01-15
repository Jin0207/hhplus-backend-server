package kr.hhplus.be.server.infrastructure.product.persistence;

import java.time.LocalDate;
import java.util.List;

import kr.hhplus.be.server.domain.product.entity.PopularProduct;

/**
 * 인기 상품 집계 쿼리용 커스텀 레포지토리
 */
public interface PopularProductCustomRepository {

    /**
     * 특정 기간의 판매량 상위 상품 집계
     * @param startDate 집계 시작일
     * @param endDate 집계 종료일
     * @param limit 상위 N개
     * @param baseDate 기준일 (배치 실행일)
     * @return 순위가 부여된 인기 상품 목록
     */
    List<PopularProduct> aggregateTopSellingProducts(
        LocalDate startDate,
        LocalDate endDate,
        int limit,
        LocalDate baseDate
    );
}
