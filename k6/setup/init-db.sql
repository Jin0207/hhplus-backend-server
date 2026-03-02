-- ============================================================
-- k6 부하테스트 - DB 초기화 스크립트
-- 실행: mysql -u application -papplication hhplus < init-db.sql
-- ============================================================

-- 이전 테스트 데이터 정리
DELETE FROM user_coupons WHERE coupon_id IN (9001, 9002, 9003, 9004);
DELETE FROM coupons WHERE id IN (9001, 9002, 9003, 9004);

-- AUTO_INCREMENT가 explicit ID insert를 허용하도록 설정
SET FOREIGN_KEY_CHECKS = 0;

-- Scenario 1: Smoke Test (50 슬롯)
INSERT INTO coupons (id, name, type, discount_value, min_order_price, valid_from, valid_to, quantity, available_quantity, status)
VALUES (9001, 'k6-smoke', 'AMOUNT', 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 50, 50, 'ACTIVE');

-- Scenario 2: Load Test (100 슬롯)
INSERT INTO coupons (id, name, type, discount_value, min_order_price, valid_from, valid_to, quantity, available_quantity, status)
VALUES (9002, 'k6-load', 'AMOUNT', 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 100, 100, 'ACTIVE');

-- Scenario 3: Stress Test (100 슬롯, 별도 쿠폰으로 Load와 독립 실행)
INSERT INTO coupons (id, name, type, discount_value, min_order_price, valid_from, valid_to, quantity, available_quantity, status)
VALUES (9003, 'k6-stress', 'AMOUNT', 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 100, 100, 'ACTIVE');

-- Scenario 4: Idempotency Test (1 슬롯)
INSERT INTO coupons (id, name, type, discount_value, min_order_price, valid_from, valid_to, quantity, available_quantity, status)
VALUES (9004, 'k6-idem', 'AMOUNT', 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 1, 'ACTIVE');

SET FOREIGN_KEY_CHECKS = 1;

-- 삽입 결과 확인
SELECT id, name, quantity, available_quantity, status FROM coupons WHERE id IN (9001, 9002, 9003, 9004);
