package kr.hhplus.be.server.domain.product.repository;

import java.time.LocalDate;
import java.util.List;

import kr.hhplus.be.server.domain.product.entity.PopularProduct;

public interface PopularProductRepository {

    /**
     * 인기 상품 저장
     */
    PopularProduct save(PopularProduct popularProduct);

    /**
     * 인기 상품 일괄 저장
     */
    List<PopularProduct> saveAll(List<PopularProduct> popularProducts);

    /**
     * 기준일로 인기 상품 조회 (순위 오름차순)
     */
    List<PopularProduct> findByBaseDate(LocalDate baseDate);

    /**
     * 가장 최신 기준일의 인기 상품 조회
     */
    List<PopularProduct> findLatest();

    /**
     * 기준일 데이터 삭제 (배치 재실행 시 멱등성 보장)
     */
    void deleteByBaseDate(LocalDate baseDate);

    /**
     * 특정 일자 이전 데이터 삭제 (정리 배치용)
     */
    void deleteByBaseDateBefore(LocalDate baseDate);
}
