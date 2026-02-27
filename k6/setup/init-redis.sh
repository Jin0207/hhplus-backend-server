#!/bin/bash
# ============================================================
# k6 부하테스트 - Redis 초기화 스크립트
# 실행: bash init-redis.sh
# ============================================================

REDIS_CLI="redis-cli"
HOST="localhost"
PORT="6379"

R="$REDIS_CLI -h $HOST -p $PORT"

echo "=== Redis 초기화 시작 ==="

# 이전 테스트 데이터 제거
echo "[1] 이전 테스트 키 삭제..."
$R DEL coupon:issued:9001 coupon:request:9001 coupon:quantity:9001
$R DEL coupon:issued:9002 coupon:request:9002 coupon:quantity:9002
$R DEL coupon:issued:9003 coupon:request:9003 coupon:quantity:9003
$R DEL coupon:issued:9004 coupon:request:9004 coupon:quantity:9004

# 수량 키 설정 (CouponService가 Redis에서 읽는 값)
echo "[2] 쿠폰 수량 키 설정..."
$R SET coupon:quantity:9001 50    # Smoke Test: 50 슬롯
$R SET coupon:quantity:9002 100   # Load Test: 100 슬롯
$R SET coupon:quantity:9003 100   # Stress Test: 100 슬롯
$R SET coupon:quantity:9004 1     # Idempotency Test: 1 슬롯

# 설정 확인
echo ""
echo "=== Redis 설정 확인 ==="
echo "coupon:quantity:9001 = $($R GET coupon:quantity:9001)  (Smoke: 50)"
echo "coupon:quantity:9002 = $($R GET coupon:quantity:9002)  (Load: 100)"
echo "coupon:quantity:9003 = $($R GET coupon:quantity:9003)  (Stress: 100)"
echo "coupon:quantity:9004 = $($R GET coupon:quantity:9004)  (Idempotency: 1)"

echo ""
echo "=== Redis 초기화 완료 ==="
