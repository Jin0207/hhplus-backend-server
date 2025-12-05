package kr.hhplus.be.server.presentation.coupon.controller;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.common.response.PageResponse;
import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.presentation.coupon.dto.request.CouponIssueRequest;
import kr.hhplus.be.server.presentation.coupon.dto.response.UserCouponResponse;
import kr.hhplus.be.server.support.response.ApiResponse;
import kr.hhplus.be.server.support.security.domain.UserPrincipal;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
@Slf4j
@Tag(name = "쿠폰 API", description = "쿠폰 조회/발급 API")
class CouponController {

    private final CouponService couponService;

    /*
     * 선착순 쿠폰 발급
     */
    @PostMapping("/issue")
    @Operation(summary = "선착순 쿠폰 발급", description = "선착순 쿠폰을 발급합니다.")
    public ApiResponse<UserCouponResponse> issueCoupon(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody CouponIssueRequest request
    ) {
        log.info("쿠폰 발급 요청: userId={}, couponId={}", 
            userPrincipal.getUserId(), request.couponId());

        UserCouponResponse response = couponService.issueCoupon(
            userPrincipal.getUserId(), 
            request.couponId()
        );

        return ApiResponse.success(response);
    }

    /**
     * 사용 가능한 쿠폰 목록 조회
     */
    @GetMapping("/available")
    public ApiResponse<PageResponse<UserCouponResponse>> getAvailableCoupons(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "crtDttm", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("사용 가능 쿠폰 페이징 조회: userId={}, page={}, size={}", 
            userPrincipal.getUserId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<UserCouponResponse> coupons = couponService.getAvailableCoupons(
            userPrincipal.getUserId(),
            pageable
        );

        return ApiResponse.success("사용 가능 쿠폰 조회 성공", PageResponse.of(coupons));
    }
    
    /**
     * 보유 쿠폰 목록 조회 (페이징)
     */
    @GetMapping("/my")
    public ApiResponse<PageResponse<UserCouponResponse>> getMyCouponsWithPaging(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "crtDttm", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("보유 쿠폰 페이징 조회: userId={}, page={}, size={}", 
            userPrincipal.getUserId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<UserCouponResponse> coupons = couponService.getUserCoupons(
            userPrincipal.getUserId(),
            pageable
        );

        return ApiResponse.success("보유 쿠폰 조회 성공", PageResponse.of(coupons));
    }
}
