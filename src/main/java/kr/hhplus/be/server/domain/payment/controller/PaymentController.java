package kr.hhplus.be.server.domain.payment.controller;

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
import kr.hhplus.be.server.domain.payment.dto.PaymentRequest;
import kr.hhplus.be.server.domain.payment.dto.PaymentResponse;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "결제 API", description = "결제 처리")
class PaymentController {
    
    @Operation(summary = "결제 요청", description = "주문에 대한 결제를 처리합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제 성공",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "결제 실패"),
        @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
        @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.ok(null);
    }
    
    @Operation(summary = "결제 조회", description = "특정 결제 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
        @Parameter(description = "결제 ID", example = "1")
        @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(null);
    }
}
