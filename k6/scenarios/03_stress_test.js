/**
 * Scenario 3 - Stress Test (한계치 탐색)
 *
 * 목적  : 단계별 VU 증가로 시스템 한계 TPS와 응답 지연 추이 관찰
 * 구성  : 300 → 600 → 1000 → 600 → 300 (ramping-vus)
 * 쿠폰  : 100 슬롯 → 초과 요청은 모두 COUPON_OUT_OF_STOCK
 * 핵심  : Overselling 발생 여부, P95 응답시간 추이, 오류율 변화
 *
 * 실행 :
 *   k6 run 03_stress_test.js
 *
 * 주의 :
 *   - ramping-vus는 동일 VU가 여러 번 반복함
 *   - 동일 VU가 두 번째 요청 시 COUPON_ALREADY_ISSUED → 정상 처리 (Redis SADD)
 *   - 실제 부하 측정을 위해 sleep 없이 실행
 *   - 사용자 풀은 1000명 (VU 최대치와 동일), 각 VU는 자신의 고정 사용자를 사용
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

// ── 커스텀 메트릭 ──────────────────────────────────────────
const issuedOk      = new Counter('coupon_issued_ok');
const issuedFail    = new Counter('coupon_issued_fail');
const outOfStock    = new Counter('coupon_out_of_stock');
const alreadyIssued = new Counter('coupon_already_issued');
const issueTime     = new Trend('coupon_issue_ms', true);

// ── 설정 ───────────────────────────────────────────────────
const BASE_URL     = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID    = 9003;
const COUPON_SLOTS = 100;
const USER_POOL    = 1000; // VU 최대치와 동일하게 유지

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 300  }, // 램프업: 0 → 300 VU
        { duration: '30s', target: 300  }, // 유지: 300 VU (1단계 관측)
        { duration: '30s', target: 600  }, // 램프업: 300 → 600 VU
        { duration: '30s', target: 600  }, // 유지: 600 VU (2단계 관측)
        { duration: '30s', target: 1000 }, // 램프업: 600 → 1000 VU
        { duration: '30s', target: 1000 }, // 유지: 1000 VU (피크)
        { duration: '30s', target: 0    }, // 램프다운
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // Stress test는 Pass/Fail이 아닌 "추이 관찰"이 목적
    // Overselling만 절대 허용 안 함 (조건부로 넉넉하게 설정)
    http_req_duration: ['p(95)<3000'],   // 피크에서도 P95 3s 이내
    http_req_failed:   ['rate<0.10'],    // HTTP 오류율 10% 이내 (비즈니스 오류 포함)
  },
};

// ── setup(): 사용자 풀 1000명 생성 ────────────────────────
export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const tokens  = [];
  const ts      = Date.now();

  console.log(`[setup] 사용자 ${USER_POOL}명 생성 중...`);

  for (let i = 0; i < USER_POOL; i++) {
    const accountId = `k6stress${ts}_${i}`;
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

    tokens.push(loginRes.json('data.token') || null);

    if (i > 0 && i % 200 === 0) {
      console.log(`[setup] ${i}/${USER_POOL} 완료`);
    }
  }

  console.log(`[setup] 완료. 유효 토큰: ${tokens.filter(Boolean).length}/${USER_POOL}`);
  return { tokens };
}

// ── default(): 쿠폰 발급 (VU별 고정 사용자) ──────────────
export default function (data) {
  // ramping-vus에서는 __VU가 1~maxVUs, 반복마다 동일 VU 재사용
  // __VU - 1 로 사용자 풀 인덱싱 (고정 매핑)
  const userIdx = (exec.vu.idInTest - 1) % USER_POOL;
  const token   = data.tokens[userIdx];

  if (!token) return;

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

  if (res.status === 200) {
    issuedOk.add(1);
  } else {
    issuedFail.add(1);
    const code = ((res.json() || {}).error || {}).code || '';
    if (code === 'COUPON_OUT_OF_STOCK')   outOfStock.add(1);
    if (code === 'COUPON_ALREADY_ISSUED') alreadyIssued.add(1);
  }

  // VU가 루프 돌 때 짧은 대기 (서버 과부하 방지 X → 스트레스 극대화)
  // 원하면 sleep(0.1)으로 조정
}

// ── teardown() ─────────────────────────────────────────────
export function teardown(data) {
  console.log('');
  console.log('=== Stress Test 완료 ===');
  console.log('단계별 TPS/Latency는 k6 출력 결과 또는 Grafana 대시보드 확인');
  console.log('');
  console.log('[정확성 검증 SQL]');
  console.log(`  SELECT available_quantity FROM coupons WHERE id = ${COUPON_ID};`);
  console.log(`  -- 기대값: 0 이상 (음수가 되면 Overselling!)`);
  console.log('');
  console.log(`  SELECT COUNT(*) FROM user_coupon WHERE coupon_id = ${COUPON_ID};`);
  console.log(`  -- 기대값: ${COUPON_SLOTS} 이하`);
}
