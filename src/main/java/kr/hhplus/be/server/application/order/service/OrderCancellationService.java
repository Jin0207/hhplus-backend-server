package kr.hhplus.be.server.application.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.application.coupon.service.CouponService;
import kr.hhplus.be.server.application.point.service.PointService;
import kr.hhplus.be.server.application.product.service.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 취소 보상 트랜잭션 서비스
 * 주문 취소 시 포인트, 쿠폰, 재고, 판매량을 원복하는 책임을 가짐
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationService {
        private final OrderService orderService;
        private final OrderDetailService orderDetailService;
        private final PointService pointService;
        private final CouponService couponService;
        private final ProductService productService;

        /**
         * 주문 취소 및 보상 트랜잭션 처리
         *
         * @param orderId 취소할 주문 ID
         */
        @Transactional
        public void cancelOrder(Long orderId) {
                log.info("[주문 취소] 시작: orderId={}", orderId);

                // 1. 주문 조회
                Order order = orderService.getOrder(orderId);

                // 2. 주문 상세 조회
                List<OrderDetail> orderDetails = orderDetailService.getOrderDetails(orderId);

                // 3. 주문 취소 처리
                Order canceledOrder = orderService.cancelOrder(orderId);
                log.info("[주문 취소] 주문 상태 변경 완료: orderId={}, status={}",
                        orderId, canceledOrder.orderStatus());

                // 4. 포인트 환불
                pointService.refundPoint(order.userId(), order.finalPrice(), "주문 취소");
                log.info("[주문 취소] 포인트 환불 완료: userId={}, amount={}",
                        order.userId(), order.finalPrice());

                // 5. 쿠폰 복구 (쿠폰을 사용한 경우만)
                if (order.couponId() != null) {
                        couponService.restoreCoupon(order.userId(), order.couponId());
                        log.info("[주문 취소] 쿠폰 복구 완료: userId={}, couponId={}",
                                order.userId(), order.couponId());
                }

                // 6. 재고 및 판매량 복구
                orderDetails.forEach(detail -> {
                        // 6-1. 재고 복구
                        productService.increaseStock(detail.productId(), detail.quantity());
                        log.info("[주문 취소] 재고 복구: productId={}, quantity={}",
                                detail.productId(), detail.quantity());

                        // 6-2. 판매량 감소
                        productService.decreaseSalesQuantity(detail.productId(), detail.quantity());
                        log.info("[주문 취소] 판매량 감소: productId={}, quantity={}",
                                detail.productId(), detail.quantity());
                });

                log.info("[주문 취소] 완료: orderId={}, 복구 항목={}",
                        orderId, orderDetails.size()
                );
        }

        /**
         * 주문 취소 가능 여부 확인
         *
         * @param orderId 확인할 주문 ID
         * @return 취소 가능 여부
         */
        public boolean canCancel(Long orderId) {
                Order order = orderService.getOrder(orderId);
                return order.canCancel();
        }
}
