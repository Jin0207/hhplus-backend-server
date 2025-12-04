package kr.hhplus.be.server.presentation.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.presentation.product.dto.request.ProductSearchRequest;
import kr.hhplus.be.server.presentation.product.dto.response.PopularProductResponse;
import kr.hhplus.be.server.presentation.product.dto.response.ProductResponse;
import kr.hhplus.be.server.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "상품 API", description = "상품 조회 API")
public class ProductController {

    private final ProductService productService;

    /*
    * 상품 목록 조회
    * @param request 상품 검색 조건 (상품명, 가격 범위, 카테고리, 판매 상태, 페이지정보)
    * @return 상품 목록 (페이지네이션 포함)
    */
    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "검색 조건에 따라 상품 목록을 조회합니다.")
    public ApiResponse<PageResponse<ProductResponse>> search(
        @Valid @ModelAttribute ProductSearchRequest request
    ) {
        PageResponse<ProductResponse> response = productService.search(request.toCommand());
        return ApiResponse.success("상품 목록 조회 성공", response);
    }

    /*
    * 최근 3일 기준 누적판매량 상위 5개 상품 조회
    * @return 최근 3일 기준 누적판매량 상위 5개 상품 목록
    */
    @GetMapping("/popular")
    @Operation(summary = "인기 상품 조회", description = "최근 3일 기준 누적판매량 상위 5개 상품을 조회합니다.")
    public ApiResponse<List<PopularProductResponse>> popularProducts() {
        List<PopularProductResponse> response = productService.findPopularProducts();
        return ApiResponse.success("인기 상품 조회 성공", response);
    }
}
