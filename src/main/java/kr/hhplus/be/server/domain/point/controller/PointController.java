package kr.hhplus.be.server.domain.point.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.point.dto.PointChargeRequest;
import kr.hhplus.be.server.domain.point.dto.PointChargeResponse;
import kr.hhplus.be.server.domain.point.dto.PointResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
@Tag(name = "포인트 API", description = "포인트 충전 및 조회 API")
class PointController {
    
    /*
     * 사용자의 ID값을 통해 포인트를 충전한다.
     */
    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "충전 성공",
            content = @Content(schema = @Schema(implementation = PointChargeResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/{userId}/charge")
    public ResponseEntity<PointChargeResponse> chargePoint(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        @PathVariable Long userId,
        @Parameter(description = "충전할 포인트 금액", required = true, example = "10000")
        @PathVariable Long chargePoint,
        @Valid @RequestBody PointChargeRequest request
    ) {
        return ResponseEntity.ok(null);
    }
    
    /*
     * 사용자의 ID값을 통해 보유 포인트를 조회한다.
     */
    @Operation(summary = "포인트 조회", description = "사용자의 포인트를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PointResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<PointResponse> getPoint(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(null);
    }
}