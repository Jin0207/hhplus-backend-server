package kr.hhplus.be.server.domain.coupon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.coupon.dto.CouponIssueRequest;
import kr.hhplus.be.server.domain.coupon.dto.CouponIssueResponse;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "쿠폰 API", description = "선착순 쿠폰 발급")
class CouponController {
    
    @Operation(summary = "선착순 쿠폰 발급", description = "선착순으로 쿠폰을 발급합니다 (동시성 제어 적용)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "발급 성공",
            content = @Content(schema = @Schema(implementation = CouponIssueResponse.class))),
        @ApiResponse(responseCode = "400", description = "발급 실패 (품절, 중복 발급 등)"),
        @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    @PostMapping("/issue")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
        @Valid @RequestBody CouponIssueRequest request
    ) {
        return ResponseEntity.ok(null);
    }
}
