package kr.hhplus.be.server.presentation.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.application.payment.service.PaymentService;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.presentation.payment.dto.PaymentResponse;
import kr.hhplus.be.server.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "결제 API", description = "결제 처리/조회 API")
class PaymentController {   

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 조회", description = "결제를 조회합니다.")
    public ApiResponse<PaymentResponse> getPayment(
        @PathVariable Long paymentId
    ) {
        
        Payment response = paymentService.getPayment(paymentId);

        return ApiResponse.success("결제 조회 성공", PaymentResponse.from(response));
    }
}
