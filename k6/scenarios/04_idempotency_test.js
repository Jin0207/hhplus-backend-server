/**
 * Scenario 4 - Idempotency Test (중복 요청 차단 검증)
 *
 * 목적 : 동일 사용자가 100번 연속 발급 요청 → 정확히 1건만 성공
 * 검증 : Redis SADD의 원자적 중복 차단이 실제로 작동하는지 확인
 * 기대 :
 *   - 1건 HTTP 200 (최초 발급 성공)
 *   - 99건 HTTP 400 + code=E401 (이미 발급된 쿠폰입니다.)
 *   - DB user_coupon 레코드: 정확히 1건
 *
 * 실행 :
 *   k6 run 04_idempotency_test.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// ── 커스텀 메트릭 ──────────────────────────────────────────
const firstSuccess    = new Counter('first_success');
const alreadyIssued   = new Counter('already_issued');
const unexpectedError = new Counter('unexpected_error');

// ── 설정 ───────────────────────────────────────────────────
const BASE_URL    = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID   = 9004;
const REPEAT_CNT  = 100; // 동일 사용자의 반복 요청 횟수

export const options = {
  scenarios: {
    idempotency: {
      executor: 'shared-iterations',
      vus: 1,          // 단 1명의 사용자
      iterations: 1,   // setup() 이후 default() 1회 실행 (내부에서 100번 반복)
      maxDuration: '60s',
    },
  },
  thresholds: {
    first_success:  ['count==1'],          // 정확히 1건만 성공
    already_issued: [`count==${REPEAT_CNT - 1}`], // 나머지 99건은 ALREADY_ISSUED
    unexpected_error: ['count==0'],        // 예상치 못한 오류 0건
  },
};

// ── setup(): 사용자 1명 생성 ───────────────────────────────
export function setup() {
  const accountId = `k6idem${Date.now()}`;
  const password  = 'Test1234!';
  const headers   = { 'Content-Type': 'application/json' };

  const signupRes = http.post(
    `${BASE_URL}/api/auth/signup`,
    JSON.stringify({ accountId, password }),
    { headers }
  );
  check(signupRes, { 'signup 200': (r) => r.status === 200 });

  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ accountId, password }),
    { headers }
  );
  const token = loginRes.json('data.token');
  check(loginRes, { 'login 200': (r) => r.status === 200 && !!token });

  console.log(`[setup] 사용자 생성: ${accountId}`);
  return { token, accountId };
}

// ── default(): 동일 사용자로 REPEAT_CNT번 연속 요청 ───────
export default function (data) {
  const { token } = data;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
  const payload = JSON.stringify({ couponId: COUPON_ID });

  console.log(`[test] ${REPEAT_CNT}번 연속 쿠폰 발급 요청 시작...`);

  for (let i = 0; i < REPEAT_CNT; i++) {
    const res = http.post(`${BASE_URL}/api/v1/coupons/issue`, payload, { headers });

    if (res.status === 200) {
      firstSuccess.add(1);
      console.log(`[iter ${i + 1}] 성공 (최초 발급)`);

    } else if (res.status === 400) {
      const code = ((res.json() || {}).error || {}).code || '';

      // 실제 에러코드: E401 ('이미 발급된 쿠폰입니다.')
      if (code === 'COUPON_ALREADY_ISSUED' || code === 'E401') {
        alreadyIssued.add(1);
      } else {
        // COUPON_OUT_OF_STOCK 등 다른 400도 있을 수 있음
        unexpectedError.add(1);
        console.error(`[iter ${i + 1}] 예상치 못한 400: code=${code} body=${res.body}`);
      }

    } else {
      unexpectedError.add(1);
      console.error(`[iter ${i + 1}] 비정상 status=${res.status} body=${res.body}`);
    }
  }
}

// ── teardown() ─────────────────────────────────────────────
export function teardown(data) {
  console.log('');
  console.log('=== Idempotency Test 완료 ===');
  console.log(`총 요청 : ${REPEAT_CNT}건 (단일 사용자)`);
  console.log('기대 결과:');
  console.log('  ✓ first_success = 1');
  console.log(`  ✓ already_issued = ${REPEAT_CNT - 1}`);
  console.log('  ✓ unexpected_error = 0');
  console.log('');
  console.log('[정확성 검증 SQL]');
  console.log(`  SELECT COUNT(*) FROM user_coupon WHERE coupon_id = ${COUPON_ID};`);
  console.log(`  -- 기대값: 1 (정확히 1건만 발급)`);
}
