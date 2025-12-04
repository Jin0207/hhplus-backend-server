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
    String category,
    String status,
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
    * 상품상태 null인 경우 기본값 세팅
    */
    public static ProductSearch createWithDefaultStatus(
        String productName,
        Integer minPrice,
        Integer maxPrice,
        ProductCategory category,
        ProductStatus status
    ) {
        // 검색 시 상태값이 null이면 자동으로 ON_SALE로 고정 (도메인 규칙)
        ProductStatus finalStatus = (status == null) ? ProductStatus.ON_SALE : status;
        
        return new ProductSearch(productName, minPrice, maxPrice, category, finalStatus);
    }
    /*
    * 상품 검색 조건을 도메인 객체로 변환
    * @return 상품 검색 조건 도메인 객체
    */
    public ProductSearch toDomain() {
        ProductCategory domainCategory = null;
        if (category != null && !category.isBlank()) {
            try {
                domainCategory = ProductCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 올바르지 않은 카테고리 값일 경우 예외 처리
                throw new BusinessException(ErrorCode.INVALID_VALUE, "상품카테고리"); 
            }
        }
        
        ProductStatus domainStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                domainStatus = ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 올바르지 않은 상태 값일 경우 예외 처리
                throw new BusinessException(ErrorCode.INVALID_VALUE, "상품상태"); 
            }
        }

        return this.createWithDefaultStatus(
            name, 
            minPrice, 
            maxPrice, 
            domainCategory, 
            domainStatus
        );
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
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            // 가격범위가 올바르지 않습니다.
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_RANGE_INVALID);
        }
    }
}
