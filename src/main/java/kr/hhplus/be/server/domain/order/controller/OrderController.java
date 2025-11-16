package kr.hhplus.be.server.domain.order.controller;

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
import kr.hhplus.be.server.domain.order.dto.OrderCreateRequest;
import kr.hhplus.be.server.domain.order.dto.OrderResponse;

@RestController
@RequestMapping("/api/vi/orders")
@Tag(name = "주문 API", description = "주문 생성 및 조회 API")
class OrderController {

    /*
     * 주문을 생성한다.
     * order/orderDetail
     */
    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "주문 생성 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (재고 부족 등)"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 상품을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody OrderCreateRequest  request
    ) {
        return ResponseEntity.ok(null);
    }
}
