package kr.hhplus.be.server.infrastructure.product;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.domain.product.repository.PopularProductRepository;
import kr.hhplus.be.server.infrastructure.product.persistence.PopularProductCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인기 상품 집계 배치 스케줄러
 * - 매일 새벽 1시에 실행
 * - 기준일(오늘) 기준 D-3 ~ D-1 기간의 판매량 상위 5개 상품 집계
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PopularProductScheduler {

    private final PopularProductRepository popularProductRepository;
    private final PopularProductCustomRepository popularProductCustomRepository;

    @Value("${popular-product.top-count:5}")
    private int topCount;

    @Value("${popular-product.period-days:3}")
    private int periodDays;

    @Value("${popular-product.cleanup-days:7}")
    private int cleanupDays;

    /**
     * 매일 새벽 1시: 인기 상품 집계
     * - 기준일: 오늘 (배치 실행일)
     * - 집계 기간: D-3 ~ D-1 (직전 3일)
     */
    @Scheduled(cron = "${popular-product.schedule.aggregate-cron:0 0 1 * * *}")
    @Transactional
    public void aggregatePopularProducts() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(periodDays);  // D-3
        LocalDate endDate = today.minusDays(1);             // D-1

        log.info("[PopularProduct] 집계 시작: baseDate={}, period={} ~ {}",
            today, startDate, endDate);

        try {
            // 1. 기존 오늘자 데이터 삭제 (멱등성 보장)
            popularProductRepository.deleteByBaseDate(today);
            log.debug("[PopularProduct] 기존 데이터 삭제 완료: baseDate={}", today);

            // 2. 판매량 상위 상품 집계
            List<PopularProduct> products = popularProductCustomRepository
                .aggregateTopSellingProducts(startDate, endDate, topCount, today);

            if (products.isEmpty()) {
                log.info("[PopularProduct] 집계 결과 없음: 해당 기간 주문 데이터 없음");
                return;
            }

            // 3. 저장
            popularProductRepository.saveAll(products);

            log.info("[PopularProduct] 집계 완료: {} 건 저장", products.size());

        } catch (Exception e) {
            log.error("[PopularProduct] 집계 실패: baseDate={}", today, e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 매일 새벽 4시: 오래된 데이터 정리
     */
    @Scheduled(cron = "${popular-product.schedule.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void cleanupOldData() {
        LocalDate cutoffDate = LocalDate.now().minusDays(cleanupDays);

        try {
            popularProductRepository.deleteByBaseDateBefore(cutoffDate);
            log.info("[PopularProduct] 정리 완료: {} 이전 데이터 삭제", cutoffDate);

        } catch (Exception e) {
            log.error("[PopularProduct] 정리 실패", e);
        }
    }
}
