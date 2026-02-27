# k6 부하테스트 실행 가이드

## 사전 조건

| 항목 | 버전 |
|------|------|
| [k6](https://k6.io/docs/get-started/installation/) | v0.50+ |
| Docker (MySQL + Redis) | docker-compose 실행 중 |
| Spring Boot 앱 | 로컬 실행 중 (기본: 8080) |

```bash
# k6 설치 (Windows)
winget install k6
# 또는
choco install k6

# k6 설치 (macOS)
brew install k6
```

---

## 실행 순서

### Step 1 - 인프라 기동

```bash
# 프로젝트 루트에서
docker-compose up -d

# 앱 실행 (별도 터미널)
./gradlew bootRun
```

### Step 2 - DB 초기화 (쿠폰 데이터 삽입)

```bash
mysql -u application -papplication hhplus < k6/setup/init-db.sql
```

### Step 3 - Redis 초기화 (수량 키 설정)

```bash
bash k6/setup/init-redis.sh
```

> **중요**: Redis에 `coupon:quantity:{id}` 키가 없으면 모든 요청이 `COUPON_ISSUE_FAILED`로 실패합니다.

### Step 4 - 시나리오 실행

각 시나리오 실행 전에 **Step 2, 3을 반드시 재실행**하여 데이터를 초기화하세요.

```bash
# 현재 위치: 프로젝트 루트

# Scenario 1: Smoke Test (50 VU, ~30초)
k6 run k6/scenarios/01_smoke_test.js

# Scenario 2: Load Test (500 VU, ~2분)
k6 run k6/scenarios/02_load_test.js

# Scenario 3: Stress Test (최대 1000 VU, ~5분)
k6 run k6/scenarios/03_stress_test.js

# Scenario 4: Idempotency Test (1 VU × 100회, ~1분)
k6 run k6/scenarios/04_idempotency_test.js
```

### Step 5 - 결과 검증 (SQL)

```bash
mysql -u application -papplication hhplus < k6/setup/verify-result.sql
```

---

## 커스텀 BASE_URL 설정

```bash
k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/01_smoke_test.js
```

---

## 쿠폰 ID 매핑

| 시나리오 | coupon_id | 슬롯 수 |
|----------|-----------|---------|
| Smoke    | 9001      | 50      |
| Load     | 9002      | 100     |
| Stress   | 9003      | 100     |
| Idempotency | 9004   | 1       |

---

## k6 출력 해석

```
✓ HTTP 200
✗ success=true

checks.........................: 95.00% ✓ 95   ✗ 5
data_received..................: 1.2 MB 10 kB/s
data_sent......................: 156 kB 1.3 kB/s
coupon_issue_ms................: avg=245ms  min=12ms   med=198ms  max=2.1s  p(90)=412ms  p(95)=489ms
coupon_issued_ok...............: 100
coupon_issued_fail.............: 400
http_req_duration..............: avg=247ms  p(95)=491ms
```

**핵심 지표**:
- `coupon_issued_ok` → 정확히 쿠폰 슬롯 수와 일치해야 함
- `coupon_issue_ms p(95)` → 500ms 이하 목표
- `coupon_already_issued` → 0이어야 함 (중복 발급 버그)

---

## Grafana 연동 (선택)

```bash
# k6 → InfluxDB → Grafana 파이프라인
k6 run --out influxdb=http://localhost:8086/k6 k6/scenarios/02_load_test.js
```

Grafana 대시보드 ID: `2587` (k6 Load Testing Results)
