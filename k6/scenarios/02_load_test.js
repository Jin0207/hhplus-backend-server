/**
 * Scenario 2 - Load Test (실제 이벤트 부하 시뮬레이션) ← 핵심 테스트
 *
 * 목적 : 프로모션 이벤트 상황 (500명 동시 경쟁 → 100장 쿠폰) 시뮬레이션
 * VU   : 500명 동시 (램프업 30초)
 * 쿠폰 : 100 슬롯 → 100건만 성공, 400건은 OUT_OF_STOCK
 * 기대 : P95 응답시간 < 500ms, Overselling 0건
 *
 * 실행 :
 *   k6 run 02_load_test.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

// ── 커스텀 메트릭 ──────────────────────────────────────────
const issuedOk        = new Counter('coupon_issued_ok');
const issuedFail      = new Counter('coupon_issued_fail');
const outOfStock      = new Counter('coupon_out_of_stock');
const alreadyIssued   = new Counter('coupon_already_issued');
const issueTime       = new Trend('coupon_issue_ms', true);
const successRate     = new Rate('coupon_success_rate');

// ── 설정 ───────────────────────────────────────────────────
const BASE_URL    = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID   = 9002;
const COUPON_SLOTS = 100;
const USER_COUNT  = 500;

export const options = {
  setupTimeout: '3m', // 500명 순차 signup+login 에 충분한 시간 확보
  scenarios: {
    load: {
      executor: 'shared-iterations',
      vus: USER_COUNT,
      iterations: USER_COUNT,   // 각 사용자 정확히 1회 시도
      maxDuration: '120s',
    },
  },
  thresholds: {
    http_req_duration:  ['p(95)<500', 'p(99)<2000'], // P95<500ms, P99<2s
    coupon_issued_ok:   [`count==${COUPON_SLOTS}`],  // 정확히 100건 성공
    coupon_already_issued: ['count==0'],             // 중복 발급 없음
  },
};

// ── setup(): 테스트 사용자 500명 생성 ──────────────────────
export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const tokens = [];
  const ts = Date.now();

  console.log(`[setup] ${USER_COUNT}명 사용자 생성 시작 (예상 ~${Math.ceil(USER_COUNT * 0.05)}초)...`);

  for (let i = 0; i < USER_COUNT; i++) {
    const accountId = `k6load${ts}_${i}`;
    const password  = 'Test1234!';

    http.post(
      `${BASE_URL}/api/auth/signup`,
      JSON.stringify({ accountId, password }),
      { headers }
    );

    const loginRes = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ accountId, password }),
      { headers }
    );

    const token = loginRes.json('data.token');
    tokens.push(token || null);

    if (i > 0 && i % 100 === 0) {
      console.log(`[setup] 진행: ${i}/${USER_COUNT}`);
    }
  }

  const validCount = tokens.filter(Boolean).length;
  console.log(`[setup] 완료. 유효 토큰: ${validCount}/${USER_COUNT}`);
  return { tokens };
}

// ── default(): 동시 쿠폰 발급 요청 ────────────────────────
export default function (data) {
  const idx   = exec.scenario.iterationInTest;
  const token = data.tokens[idx];

  if (!token) {
    console.error(`[iter ${idx}] 토큰 없음, 스킵`);
    return;
  }

  const startMs = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/coupons/issue`,
    JSON.stringify({ couponId: COUPON_ID }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
    }
  );
  issueTime.add(Date.now() - startMs);

  const ok = res.status === 200;
  successRate.add(ok);

  if (ok) {
    issuedOk.add(1);
  } else {
    issuedFail.add(1);

    // 실패 원인 분류
    const body = res.json() || {};
    const code  = (body.error && body.error.code) || '';

    if (code === 'COUPON_OUT_OF_STOCK') {
      outOfStock.add(1);
    } else if (code === 'COUPON_ALREADY_ISSUED') {
      alreadyIssued.add(1);
      // ALREADY_ISSUED는 버그 시나리오이므로 로그
      console.error(`[iter ${idx}] COUPON_ALREADY_ISSUED! 동일 유저가 두 번 요청됨`);
    } else {
      // 예상치 못한 오류 로그
      console.warn(`[iter ${idx}] 미분류 실패: status=${res.status} code=${code}`);
    }
  }
}

// ── teardown() ─────────────────────────────────────────────
export function teardown(data) {
  console.log('');
  console.log('=== Load Test 완료 ===');
  console.log(`총 요청 : ${USER_COUNT}건`);
  console.log(`쿠폰 슬롯 : ${COUPON_SLOTS}장`);
  console.log('');
  console.log('[정확성 검증 SQL]');
  console.log(`  SELECT COUNT(*) FROM user_coupon WHERE coupon_id = ${COUPON_ID};`);
  console.log(`  -- 기대값: ${COUPON_SLOTS} (Overselling 없으면 정확히 100)`);
  console.log('');
  console.log(`  SELECT user_id, COUNT(*) FROM user_coupon WHERE coupon_id = ${COUPON_ID}`);
  console.log(`  GROUP BY user_id HAVING COUNT(*) > 1;`);
  console.log(`  -- 기대값: 0행 (중복 발급 없음)`);
}
