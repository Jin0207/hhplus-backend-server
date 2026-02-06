package kr.hhplus.be.server.application.product.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.presentation.product.dto.response.PopularProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 랭킹 서비스 (Redis Sorted Set 기반)
 *
 * Redis 자료구조 활용:
 * - Sorted Set (ranking:products:daily:{yyyyMMdd}): 일별 판매량 누적
 * - Sorted Set (ranking:products:aggregated): ZUNIONSTORE로 합산된 랭킹 (캐시)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;

    private static final String RANKING_KEY_PREFIX = "ranking:products:daily:";
    private static final String AGGREGATED_KEY = "ranking:products:aggregated";
    private static final String CACHE_KEY = "ranking:products:cache";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DEFAULT_TOP_COUNT = 5;
    private static final int DEFAULT_PERIOD_DAYS = 3;
    private static final long RANKING_KEY_TTL_DAYS = 7;
    private static final long CACHE_TTL_SECONDS = 60; // 1분 캐시

    /**
     * 판매량 증가 (주문 완료 시)
     * - Sorted Set: ZINCRBY로 원자적 판매량 증가
     * - 캐시 무효화
     */
    public void incrementSalesScore(Long productId, Integer quantity) {
        String dailyKey = getDailyKey(LocalDate.now());
        try {
            redisTemplate.opsForZSet().incrementScore(
                dailyKey,
                String.valueOf(productId),
                quantity.doubleValue()
            );

            // TTL 설정 (매번 갱신하여 경쟁조건 방지)
            redisTemplate.expire(dailyKey, RANKING_KEY_TTL_DAYS, TimeUnit.DAYS);

            // 캐시 무효화 (다음 조회 시 재계산)
            invalidateCache();

            log.debug("[랭킹] 판매량 증가: productId={}, quantity={}", productId, quantity);
        } catch (Exception e) {
            log.warn("[랭킹] 판매량 증가 실패 (무시됨): productId={}", productId, e);
        }
    }

    /**
     * 판매량 감소 (주문 취소 시)
     * - 주문 생성일 기준으로 해당 일자의 점수 감소
     */
    public void decrementSalesScore(Long productId, Integer quantity, LocalDate orderDate) {
        String dailyKey = getDailyKey(orderDate);
        try {
            Double currentScore = redisTemplate.opsForZSet().score(dailyKey, String.valueOf(productId));

            if (currentScore != null && currentScore > 0) {
                double newScore = Math.max(0, currentScore - quantity);
                if (newScore > 0) {
                    redisTemplate.opsForZSet().add(dailyKey, String.valueOf(productId), newScore);
                } else {
                    redisTemplate.opsForZSet().remove(dailyKey, String.valueOf(productId));
                }

                // 캐시 무효화
                invalidateCache();

                log.debug("[랭킹] 판매량 감소: productId={}, quantity={}, orderDate={}",
                    productId, quantity, orderDate);
            }
        } catch (Exception e) {
            log.warn("[랭킹] 판매량 감소 실패 (무시됨): productId={}", productId, e);
        }
    }

    /**
     * 판매량 감소 (주문 취소 시) - 오늘 날짜 기준 (하위 호환)
     */
    public void decrementSalesScore(Long productId, Integer quantity) {
        decrementSalesScore(productId, quantity, LocalDate.now());
    }

    /**
     * 인기 상품 랭킹 조회 (최근 N일 기준 상위 M개)
     * - ZUNIONSTORE로 Redis 내에서 합산 (오버헤드 최소화)
     * - 짧은 TTL 캐시로 반복 조회 방지
     */
    public List<PopularProductResponse> getTopRankingProducts() {
        return getTopRankingProducts(DEFAULT_TOP_COUNT, DEFAULT_PERIOD_DAYS);
    }

    /**
     * 인기 상품 랭킹 조회 (파라미터 지정)
     */
    public List<PopularProductResponse> getTopRankingProducts(int topCount, int periodDays) {
        try {
            // 1. 캐시 확인
            Set<ZSetOperations.TypedTuple<String>> cached = redisTemplate.opsForZSet()
                .reverseRangeWithScores(CACHE_KEY, 0, topCount - 1);

            if (cached != null && !cached.isEmpty()) {
                log.debug("[랭킹] 캐시 Hit: {} 건", cached.size());
                return buildResponse(cached);
            }

            // 2. ZUNIONSTORE로 합산 (Redis 내에서 처리)
            String[] dailyKeys = buildDailyKeys(periodDays);

            // 일별 키가 모두 비어있는지 확인
            boolean hasData = false;
            for (String key : dailyKeys) {
                Long size = redisTemplate.opsForZSet().zCard(key);
                if (size != null && size > 0) {
                    hasData = true;
                    break;
                }
            }

            if (!hasData) {
                log.debug("[랭킹] 랭킹 데이터 없음");
                return Collections.emptyList();
            }

            // ZUNIONSTORE: 여러 Sorted Set 합산
            redisTemplate.opsForZSet().unionAndStore(dailyKeys[0],
                List.of(dailyKeys).subList(1, dailyKeys.length), AGGREGATED_KEY);
            redisTemplate.expire(AGGREGATED_KEY, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

            // 3. 상위 N개 조회
            Set<ZSetOperations.TypedTuple<String>> topProducts = redisTemplate.opsForZSet()
                .reverseRangeWithScores(AGGREGATED_KEY, 0, topCount - 1);

            if (topProducts == null || topProducts.isEmpty()) {
                log.debug("[랭킹] 합산 후 데이터 없음");
                return Collections.emptyList();
            }

            // 4. 캐시에 저장 (짧은 TTL)
            for (ZSetOperations.TypedTuple<String> tuple : topProducts) {
                redisTemplate.opsForZSet().add(CACHE_KEY, tuple.getValue(), tuple.getScore());
            }
            redisTemplate.expire(CACHE_KEY, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

            // 5. 응답 생성
            return buildResponse(topProducts);

        } catch (Exception e) {
            log.warn("[랭킹] Redis 랭킹 조회 실패", e);
            return Collections.emptyList();
        }
    }

    /**
     * 캐시 무효화
     */
    private void invalidateCache() {
        try {
            redisTemplate.delete(CACHE_KEY);
            redisTemplate.delete(AGGREGATED_KEY);
        } catch (Exception e) {
            log.warn("[랭킹] 캐시 무효화 실패", e);
        }
    }

    /**
     * 일별 키 배열 생성
     */
    private String[] buildDailyKeys(int periodDays) {
        LocalDate today = LocalDate.now();
        String[] keys = new String[periodDays];
        for (int i = 0; i < periodDays; i++) {
            keys[i] = getDailyKey(today.minusDays(i));
        }
        return keys;
    }

    /**
     * Sorted Set 결과로부터 응답 생성
     */
    private List<PopularProductResponse> buildResponse(Set<ZSetOperations.TypedTuple<String>> tuples) {
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 상품 ID 추출
        List<Long> productIds = tuples.stream()
            .map(tuple -> Long.parseLong(tuple.getValue()))
            .toList();

        // DB에서 상품 정보 조회
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::id, p -> p));

        // 응답 생성 (순서 유지)
        return tuples.stream()
            .map(tuple -> {
                Long productId = Long.parseLong(tuple.getValue());
                Product product = productMap.get(productId);
                if (product == null) {
                    return null;
                }
                return new PopularProductResponse(
                    product.id(),
                    product.productName(),
                    product.price(),
                    product.stock(),
                    product.category().name(),
                    product.status(),
                    tuple.getScore().intValue()
                );
            })
            .filter(response -> response != null)
            .toList();
    }

    /**
     * 일별 랭킹 키 생성
     */
    private String getDailyKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }

    /**
     * 랭킹 데이터 존재 여부 확인
     */
    public boolean hasRankingData() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < DEFAULT_PERIOD_DAYS; i++) {
            String dailyKey = getDailyKey(today.minusDays(i));
            Long size = redisTemplate.opsForZSet().zCard(dailyKey);
            if (size != null && size > 0) {
                return true;
            }
        }
        return false;
    }
}
