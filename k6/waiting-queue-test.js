import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const enterSuccess = new Counter('queue_enter_success');
const pollSuccess = new Counter('queue_poll_success');
const failureRate = new Rate('queue_failure_rate');

export const options = {
  scenarios: {
    queue_spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '3s', target: 100 },  // 3초 만에 100명 진입 폭주
        { duration: '5s', target: 200 },  // 200명 유지
        { duration: '2s', target: 0 },    // 종료
      ],
    },
  },
  thresholds: {
    'queue_failure_rate': ['rate<0.01'], // 에러율 1% 미만이어야 함
    'http_req_duration': ['p(95)<100'],   // 95% 요청이 100ms 이내여야 함 (Redis 초고속 처리)
  },
};

const QUEUE_SERVICE_URL = __ENV.QUEUE_API_URL || 'http://localhost:8081/api/v1';
const CONCERT_ID = 1;

export default function () {
  const userId = 10000 + __VU * 100 + __ITER;

  // 1. 대기열 진입 요청
  const enterPayload = JSON.stringify({
    userId: userId,
    concertId: CONCERT_ID,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const enterRes = http.post(`${QUEUE_SERVICE_URL}/queue/tokens`, enterPayload, params);
  const enterOk = check(enterRes, {
    'enter queue 200 OK': (r) => r.status === 200,
  });

  if (!enterOk) {
    failureRate.add(1);
    return;
  }

  enterSuccess.add(1);
  failureRate.add(0);

  const resJson = JSON.parse(enterRes.body);
  const token = resJson.data.token;

  // 2. 내 순번 폴링 (0.5초 대기 후 상태 확인)
  sleep(0.5);

  const statusParams = {
    headers: {
      'Content-Type': 'application/json',
      'X-Queue-Token': token,
    },
  };

  const statusRes = http.get(`${QUEUE_SERVICE_URL}/queue/status?concertId=${CONCERT_ID}`, statusParams);
  const pollOk = check(statusRes, {
    'poll status 200 OK': (r) => r.status === 200,
  });

  if (pollOk) {
    pollSuccess.add(1);
    failureRate.add(0);
  } else {
    failureRate.add(1);
  }
}
