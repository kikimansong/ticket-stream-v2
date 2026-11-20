import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    dashboard_sustained_load: {
      executor: 'constant-vus',
      vus: 20,
      duration: '20s',
    },
  },
};

const QUEUE_URL = 'http://localhost:8080/api/v2/queue';
const SEAT_URL = 'http://localhost:8080/api/v2/seats';

export default function () {
  // 1. 좌석 목록 실시간 조회
  const seatRes = http.get(`${SEAT_URL}?concertId=1`, {
    headers: { 'X-Queue-Token': 'TEST_TOKEN' },
  });
  check(seatRes, { 'get seats 200': (r) => r.status === 200 });

  // 2. 대기열 진입
  const payload = JSON.stringify({ userId: 1000 + __VU, concertId: 1 });
  const queueRes = http.post(`${QUEUE_URL}/tokens`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(queueRes, { 'queue enter 200': (r) => r.status === 200 });

  sleep(0.3);
}
