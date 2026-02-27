/**
 * Scenario 1 - Smoke Test (정상 동작 확인)
 *
 * 목적 : 기본 선착순 쿠폰 발급이 정확하게 동작하는지 확인
 * VU   : 50명 동시 (쿠폰 수량 = 50)
 * 기대 : 정확히 50건 성공, 0건 초과 발급, 0건 중복
 *
 * 실행 :
 *   k6 run 01_smoke_test.js
 *   k6 run -e BASE_URL=http://localhost:8080 01_smoke_test.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

// ── 커스텀 메트릭 ──────────────────────────────────────────
const issuedOk    = new Counter('coupon_issued_ok');
const issuedFail  = new Counter('coupon_issued_fail');
const successRate = new Rate('coupon_success_rate');
const issueTime   = new Trend('coupon_issue_ms', true);

// ── 설정 ───────────────────────────────────────────────────
const BASE_URL   = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID  = 9001;
const USER_COUNT = 50;

export const options = {
  scenarios: {
    smoke: {
      executor: 'shared-iterations', // 총 N회를 VU들이 나눠서 처리
      vus: USER_COUNT,
      iterations: USER_COUNT,        // VU 수 = 반복 수 → 각 VU가 정확히 1회 실행
      maxDuration: '60s',
    },
  },
  thresholds: {
    http_req_duration:    ['p(95)<500'],   // P95 응답시간 500ms 이하
    http_req_failed:      ['rate<0.01'],   // HTTP 오류율 1% 미만 (4xx/5xx 포함이므로 참고용)
    coupon_success_rate:  ['rate>0.99'],   // 발급 성공률 99% 이상 (모두 성공해야 하는 시나리오)
  },
};

// ── setup(): 테스트 사용자 생성 ────────────────────────────
export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const tokens = [];

  console.log(`[setup] ${USER_COUNT}명 사용자 생성 시작...`);

  for (let i = 0; i < USER_COUNT; i++) {
    const accountId = `k6smoke${Date.now()}_${i}`;
    const password  = 'Test1234!';

    // 회원가입
    const signupRes = http.post(
      `${BASE_URL}/api/auth/signup`,
      JSON.stringify({ accountId, password }),
      { headers }
    );
    if (signupRes.status !== 200) {
      console.error(`[setup] signup 실패 (${i}): status=${signupRes.status}`);
    }

    // 로그인
    const loginRes = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ accountId, password }),
      { headers }
    );
    const token = loginRes.json('data.token');
    if (!token) {
      console.error(`[setup] login 토큰 취득 실패 (${i}): ${loginRes.body}`);
    }
    tokens.push(token);
  }

  console.log(`[setup] 완료. tokens=${tokens.filter(Boolean).length}/${USER_COUNT}`);
  return { tokens };
}

// ── default(): 쿠폰 발급 요청 ─────────────────────────────
export default function (data) {
  const idx   = exec.scenario.iterationInTest; // 0-based unique iteration index
  const token = data.tokens[idx];

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

  const ok = check(res, {
    'HTTP 200': (r) => r.status === 200,
    'success=true': (r) => r.json('success') === true,
  });

  if (ok) {
    issuedOk.add(1);
  } else {
    issuedFail.add(1);
    console.warn(`[VU${exec.vu.idInTest}] 발급 실패: status=${res.status} body=${res.body}`);
  }
  successRate.add(ok);
}

// ── teardown(): 최종 요약 ──────────────────────────────────
export function teardown(data) {
  console.log('');
  console.log('=== Smoke Test 완료 ===');
  console.log(`사용자 수 : ${data.tokens.length}`);
  console.log(`쿠폰 수량 : ${USER_COUNT}`);
  console.log('');
  console.log('[정확성 검증 SQL]');
  console.log(`  SELECT COUNT(*) FROM user_coupon WHERE coupon_id = ${COUPON_ID};`);
  console.log(`  -- 기대값: ${USER_COUNT}`);
}
