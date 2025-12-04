package kr.hhplus.be.server.presentation.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "사용자 API", description = "사용자 조회 API")
public class UserController {
    private final UserFacade userFacade;

    /**
     * 포인트 충전
     * POST /api/users/{userId}/points/charge
     */
    @PostMapping("/{userId}/points/charge")
    public ApiResponse<UserResponse> chargePoint(
            @PathVariable Long userId,
            @Valid @RequestBody PointChargeRequest request) {
        UserResponse response = userFacade.chargePoint(userId, request.amount(), null);
        return ApiResponse.success("포인트 충전 성공", response);
    }

    /**
     * 포인트 사용
     * POST /api/users/{userId}/points/use
     */
    @PostMapping("/{userId}/points/use")
    public ApiResponse<UserResponse> usePoint(
            @PathVariable Long userId,
            @Valid @RequestBody PointChargeRequest request) {
        UserResponse response = userFacade.usePoint(userId, request.amount());
        return ApiResponse.success("포인트 사용 성공", response);
    }

    /**
     * 포인트 잔액 조회
     * GET /api/users/{userId}/points
     */
    @GetMapping("/{userId}/points")
    public ApiResponse<Integer> getPointBalance(@PathVariable Long userId) {
        Integer balance = userFacade.getPointBalance(userId);
        return ApiResponse.success("포인트 잔액 조회 성공", balance);
    }

    /**
     * 포인트 이력 조회
     * GET /api/users/{userId}/points/history
     */
    @GetMapping("/{userId}/points/history")
    public ApiResponse<List<PointHistoryResponse>> getPointHistory(
            @PathVariable Long userId) {
        List<PointHistoryResponse> responses = userFacade.getPointHistory(userId);
        return ApiResponse.success("포인트 내역 조회 성공",responses);
    }
}
