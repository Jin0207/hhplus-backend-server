package kr.hhplus.be.server.presentation.coupon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.presentation.coupon.dto.CouponIssueRequest;
import kr.hhplus.be.server.presentation.coupon.dto.CouponIssueResponse;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "쿠폰 API", description = "쿠폰 조회/발급 API")
class CouponController {
    
    @PostMapping("/issue")
    @Operation(summary = "선착순 쿠폰 발급", description = "선착순 쿠폰을 발급합니다.")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
        @Valid @RequestBody CouponIssueRequest request
    ) {
        return ResponseEntity.ok(null);
    }
}
