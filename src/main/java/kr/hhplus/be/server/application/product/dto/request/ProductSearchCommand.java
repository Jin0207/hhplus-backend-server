package kr.hhplus.be.server.application.product.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;
/*
* 상품 검색 조건
* @param name 상품명
* @param category 카테고리
* @param status 상태
* @param minPrice 최소가격
* @param maxPrice 최대가격
* @param page 페이지
* @param size 페이지 크기
*/
public record ProductSearchCommand(
    String name,
    ProductCategory category,
    ProductStatus status,
    Integer minPrice,
    Integer maxPrice,
    Integer page,
    Integer size
) {
    public ProductSearchCommand {
        validatePriceRange(minPrice, maxPrice);
    }
    /*
    * 페이지 정보를 페이지 요청 객체로 변환
    * @return 페이지 요청 객체
    */
    public Pageable toPageable() {
        return PageRequest.of(
                pageOrDefault(),
                sizeOrDefault(),
                Sort.by("id").descending()
        );
    }

    /*
    * 상품 검색 조건을 도메인 객체로 변환
    * @return 상품 검색 조건 도메인 객체
    */
    public ProductSearch toDomain() {
        return new ProductSearch(name, minPrice, maxPrice, category, status);
    }

    /*
    * 페이지 기본값(0) 반환
    * @return 페이지 기본값(0)
    */
    public int pageOrDefault() {
        return page != null ? page : 0;
    }

    /*
    * 페이지 크기 기본값(10) 반환
    * @return 페이지 크기 기본값(10)
    */
    public int sizeOrDefault() {
        return size != null ? size : 10;
    }

    /*
    * 가격 범위 검증
    * @param minPrice 최소가격
    * @param maxPrice 최대가격
    */
    private void validatePriceRange(Integer minPrice, Integer maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return;
        }

        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_RANGE_INVALID);
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_RANGE_INVALID);
        }
    }
}
