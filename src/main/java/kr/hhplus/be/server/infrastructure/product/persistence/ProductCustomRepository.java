package kr.hhplus.be.server.infrastructure.product.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.hhplus.be.server.domain.product.entity.ProductSearch;

public interface ProductCustomRepository {
    /*
    * 상품 검색
    * @param search 상품 검색 조건
    * @param pageable 페이지 정보
    * @return 상품 목록
    */
    Page<ProductEntity> findBySearch(ProductSearch search, Pageable pageable);

    /*
    * 최근 3일 동안 누적판매량 기준 상위 5 개 상품 조회.
    * @return 인기 상품 목록
    */
    List<ProductEntity> findPopularProducts();
}
