package kr.hhplus.be.server.presentation.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.order.dto.request.OrderCreateRequest;
import kr.hhplus.be.server.application.order.dto.response.OrderResponse;
import kr.hhplus.be.server.application.order.facade.OrderFacade;
import kr.hhplus.be.server.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@Tag(name = "주문 API", description = "주문 생성 API")
@RequestMapping("/api/vi/orders")
@RestController
@RequiredArgsConstructor
class OrderController {
    private final OrderFacade orderFacade;

    /**
     * 주문 생성 및 결제
     * POST /api/orders
     */
    @Operation(summary = "주문 생성", description = "상품 주문 및 결제를 처리합니다")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        
        OrderResponse response = orderFacade.createOrderWithPayment(userId, request);
        return ApiResponse.success("주문 생성 성공", response);
    }
}
