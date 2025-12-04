package kr.hhplus.be.server.domain.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.infrastructure.product.persistence.ProductEntity;

public interface ProductRepository {

    /*
    * 상품 단건 검색
    * @param productId
    * @return Product
    */
    Product save(Product product);
    /*

    /*
    * 상품 단건 검색
    * @param productId
    * @return Product
    */
    Optional<Product> findById(Long productId);
    /*
    * 상품 검색
    * @param search 상품 검색 조건
    * @param pageable
    * @return 현재 페이지 상품 및 총 건수
    */
    Page<ProductEntity> findBySearch(ProductSearch search, Pageable pageable);
    /*
    * 최근 3일 동안 누적판매량 기준 상위 5 개 상품 조회.
    * @return 인기 상품 목록
    */
    List<Product> findPopularProducts();
}
