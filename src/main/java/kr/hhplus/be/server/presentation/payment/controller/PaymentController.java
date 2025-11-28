package kr.hhplus.be.server.presentation.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.presentation.payment.dto.PaymentRequest;
import kr.hhplus.be.server.presentation.payment.dto.PaymentResponse;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "결제 API", description = "결제 처리/조회 API")
class PaymentController {
    
    @PostMapping
    @Operation(summary = "결제 처리", description = "결제를 처리합니다.")
    public ResponseEntity<PaymentResponse> processPayment(
        @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.ok(null);
    }
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 조회", description = "결제를 조회합니다.")
    public ResponseEntity<PaymentResponse> getPayment(
        @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(null);
    }
}
