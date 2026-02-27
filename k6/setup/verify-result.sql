-- ============================================================
-- k6 부하테스트 - 결과 검증 쿼리
-- 실행: mysql -u application -papplication hhplus < verify-result.sql
-- ============================================================

-- 1. 쿠폰별 발급 건수 (coupon ID 기준)
SELECT
    c.id AS coupon_id,
    c.name,
    c.quantity AS 쿠폰_총수량,
    c.available_quantity AS 남은_수량,
    (c.quantity - c.available_quantity) AS DB_차감수량,
    COUNT(uc.coupon_id) AS UserCoupon_발급건수
FROM coupons c
LEFT JOIN user_coupons uc ON c.id = uc.coupon_id
WHERE c.id IN (9001, 9002, 9003, 9004)
GROUP BY c.id, c.name, c.quantity, c.available_quantity;

-- 2. 중복 발급 검사 (동일 userId + couponId 조합이 2개 이상이면 이상)
SELECT
    uc.user_id,
    uc.coupon_id,
    COUNT(*) AS 발급_횟수
FROM user_coupons uc
WHERE uc.coupon_id IN (9001, 9002, 9003, 9004)
GROUP BY uc.user_id, uc.coupon_id
HAVING COUNT(*) > 1;

-- 3. 테스트 사용자 수 확인
SELECT COUNT(*) AS k6_사용자수 FROM users WHERE account_id LIKE 'k6%';
