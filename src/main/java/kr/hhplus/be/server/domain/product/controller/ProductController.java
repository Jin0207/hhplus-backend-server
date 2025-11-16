package kr.hhplus.be.server.domain.product.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.domain.product.dto.PopularProductResponse;
import kr.hhplus.be.server.domain.product.dto.ProductResponse;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "상품 API", description = "상품 조회 API")
class ProductController {

    /*
     * 판매상태에 따른 상품목록을 조회한다.
     */
    @Operation(summary = "상품 목록 조회", description = "전체 상품 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
        @Parameter(description = "카테고리 필터", example = "전자제품")
        @RequestParam(required = false) String category,
        @Parameter(description = "판매 상태 필터", example = "ON_SALE")
        @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(List.of());
    }

    /*
     * 최근 3일 기준 누적판매량 상위 5개 상품을 조회한다.
     */
    @Operation(summary = "인기 상품 조회", description = "판매량 기준 인기 상품을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                [
                    {
                        "id": 1,
                        "name": "맥북 프로 16인치",
                        "price": 3500000,
                        "stock": 150,
                        "category": "전자제품",
                        "status": "ON_SALE",
                        "totalSales": 300
                    },
                    {
                        "id": 2,
                        "name": "아이폰 15",
                        "price": 1500000,
                        "stock": 80,
                        "category": "전자제품",
                        "status": "ON_SALE",
                        "totalSales": 180
                    },
                    {
                        "id": 3,
                        "name": "아이폰PRO 15",
                        "price": 2000000,
                        "stock": 80,
                        "category": "전자제품",
                        "status": "ON_SALE",
                        "totalSales": 160
                    },
                    {
                        "id": 4,
                        "name": "갤럭시 플립7",
                        "price": 1500000,
                        "stock": 80,
                        "category": "전자제품",
                        "status": "ON_SALE",
                        "totalSales": 150
                    },
                    {
                        "id": 5,
                        "name": "갤럭시 이온",
                        "price": 2500000,
                        "stock": 80,
                        "category": "전자제품",
                        "status": "ON_SALE",
                        "totalSales": 130
                    }
                ]
            """)
        ))
    })
    @GetMapping("/popular")
    public ResponseEntity<List<PopularProductResponse>> getPopularProducts(
    ) {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
