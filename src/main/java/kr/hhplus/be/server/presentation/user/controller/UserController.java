package kr.hhplus.be.server.presentation.user.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.point.response.PointHistoryResponse;
import kr.hhplus.be.server.application.user.facade.UserFacade;
import kr.hhplus.be.server.presentation.user.dto.request.PointChargeRequest;
import kr.hhplus.be.server.presentation.user.dto.response.UserResponse;
import kr.hhplus.be.server.support.response.ApiResponse;
import kr.hhplus.be.server.support.security.domain.UserPrincipal;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "사용자 API", description = "사용자 조회 API")
public class UserController {
    private final UserFacade userFacade;

    /**
     * 포인트 충전
     */
    @PostMapping("/points/charge")
    public ApiResponse<UserResponse> chargePoint(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PointChargeRequest request) {
        UserResponse response = userFacade.chargePoint(
            userPrincipal.getUserId(), 
            request.amount(), 
            null
        );
        return ApiResponse.success("포인트 충전 성공", response);
    }

    /**
     * 포인트 사용
     */
    @PostMapping("/points/use")
    public ApiResponse<UserResponse> useMyPoint(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PointChargeRequest request) {
        
        UserResponse response = userFacade.usePoint(
            userPrincipal.getUserId(), 
            request.amount()
        );
        return ApiResponse.success("포인트 사용 성공", response);
    }

    /**
     * 포인트 잔액 조회
     */
    @GetMapping("/points")
    public ApiResponse<Long> getMyPointBalance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long balance = userFacade.getPointBalance(userPrincipal.getUserId());
        return ApiResponse.success("포인트 잔액 조회 성공", balance);
    }

    /**
     * 포인트 이력 조회
     */
    @GetMapping("/points/history")
    public ApiResponse<List<PointHistoryResponse>> getMyPointHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<PointHistoryResponse> responses = userFacade.getPointHistory(
            userPrincipal.getUserId()
        );
        return ApiResponse.success("포인트 내역 조회 성공", responses);
    }
}
