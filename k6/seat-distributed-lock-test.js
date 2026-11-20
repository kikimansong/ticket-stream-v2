import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// 메트릭 정의
const successCount = new Counter('seat_reserve_success');
const blockedCount = new Counter('seat_reserve_blocked_409');
const otherErrorCount = new Counter('seat_reserve_error_other');

// 30명 동시 요청 (단 1개 좌석 선점 경쟁)
const VUS = 30;

export const options = {
  scenarios: {
    single_seat_distributed_lock: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '10s',
    },
  },
  thresholds: {
    // 1등 단 1명만 선점 성공 (200 OK)
    'seat_reserve_success': ['count==1'],
    // 나머지 29명은 409 Conflict(선점 충돌)로 즉각 Fast-Fail 차단되어야 함
    'seat_reserve_blocked_409': [`count==${VUS - 1}`],
    // v1과 달리 DB 락 타임아웃(500 에러)은 0건이어야 함
    'seat_reserve_error_other': ['count==0'],
  },
};

const SEAT_SERVICE_URL = __ENV.SEAT_API_URL || 'http://localhost:8080/api/v2';
const CONCERT_ID = 1;
const TARGET_SEAT_ID = 1;

export function setup() {
  console.log(`\n======================================================`);
  console.log(`[k6 Setup] 공연 ID ${CONCERT_ID} 좌석 초기화...`);
  const resetRes = http.post(`${SEAT_SERVICE_URL}/seats/reset/${CONCERT_ID}`);
  check(resetRes, {
    'setup: seats reset successfully': (r) => r.status === 200,
  });
  console.log(`[k6 Setup] 좌석 초기화 완료! 동시 선점 테스트 시작 (${VUS}명 동시 경합)`);
  console.log(`======================================================\n`);
}

export default function () {
  const userId = 5000 + __VU;

  const payload = JSON.stringify({
    userId: userId,
    concertId: CONCERT_ID,
    seatId: TARGET_SEAT_ID,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const res = http.post(`${SEAT_SERVICE_URL}/seats/reserve`, payload, params);

  if (res.status === 200) {
    successCount.add(1);
    console.log(`[VU ${__VU}] 🏆 좌석 선점 1등 성공! (200 OK) -> userId: ${userId}`);
  } else if (res.status === 409) {
    blockedCount.add(1);
  } else {
    otherErrorCount.add(1);
    console.error(`[VU ${__VU}] ❌ 예상치 못한 에러: status=${res.status}, body=${res.body}`);
  }

  check(res, {
    'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
}
