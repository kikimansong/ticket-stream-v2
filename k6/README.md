# Ticket Stream v2 - k6 부하 테스트 가이드 🎫

이 문서는 **Grafana k6**를 활용하여 Ticket Stream v2의 **대기열 서비스(Redis ZSet)**, **Redisson 분산 락(In-Memory Fast-Fail)**, **Kafka 이벤트 기반 최종 일관성**의 성능과 정합성을 검증하는 실행 가이드입니다.

---

## 1. 사전 준비 (Pre-requisites)

부하 테스트를 실행하기 전, 백엔드 서비스와 인프라 컨테이너가 정상 구동 중이어야 합니다.

```powershell
# 1. 인프라 컨테이너 구동 (PostgreSQL, Redis, Kafka)
docker-compose up -d

# 2. 백엔드 서비스 구동 (각 터미널별)
.\gradlew.bat :services:waiting-service:bootRun
.\gradlew.bat :services:seat-service:bootRun
.\gradlew.bat :services:order-service:bootRun
.\gradlew.bat :services:payment-service:bootRun

# 3. k6 설치 확인
k6 version
```

---

## 2. 부하 테스트 시나리오 요약

| 시나리오 | 동시 요청 (VUs) | 대상 서비스 | 핵심 검증 목표 | 기대 결과 |
| :--- | :---: | :--- | :--- | :--- |
| **1. Redisson 분산 락 선점 검증** | **30 ~ 100명** | `seat-service` (Port 8083) | 단일 좌석 동시 선점 시 1등만 200 OK, 나머지는 409 Conflict로 즉시 차단 (Fast-Fail) | • 200 OK: **1건**<br/>• 409 Conflict: **`VUs - 1`건**<br/>• 500 에러: **0건 (타임아웃 없음)** |
| **2. 대기열 트래픽 흡수 검증** | **200명 Ramping** | `waiting-service` (Port 8081) | Redis ZSet 메모리 기반으로 수만 RPS 대기열 등록 및 순번 폴링 처리 | • 응답 시간: **95%가 100ms 이내**<br/>• 에러율: **0%** |
| **3. 실시간 대시보드 모니터링** | **20명 지속** | 전체 서비스 | 웹 브라우저에서 실시간 RPS 및 응답 지연 시간 그래프 관찰 | • 20초간 안정적인 트래픽 처리 확인 |

---

## 3. k6 실행 방법

### 3.1 [시나리오 1] Redisson 분산 락 선점 테스트 (30명 동시 요청)
단 1개의 좌석(ID: 1)에 30명이 동시에 임시 선점(`POST /api/v2/seats/reserve`)을 시도합니다.

```powershell
k6 run k6/seat-distributed-lock-test.js
```

#### CLI 옵션으로 가상 사용자(VUs) 수 즉석 변경:
```powershell
# 100명 동시 요청으로 스케일업 실행
k6 run --vus 100 --iterations 100 k6/seat-distributed-lock-test.js
```

> **💡 v1과의 결정적 차이**:  
> - **v1**: 100명 동시 요청 시 DB Row Lock 때문에 HikariCP가 고갈되어 수십 건의 3초 타임아웃(500 Internal Server Error)이 발생했습니다.
> - **v2**: Redisson 분산 락이 1등만 통과시키고 나머지를 메모리 레벨에서 즉각 409 Conflict로 쳐내므로, **500 에러가 0건**이며 응답 속도가 획기적으로 개선됩니다.

---

### 3.2 [시나리오 2] 대기열(Redis ZSet) 트래픽 스파이크 테스트 (최대 200명 동시 진입)
수백 명의 사용자가 순간적으로 대기열 진입 토큰을 발급받고 순번을 확인하는 테스트입니다.

```powershell
k6 run k6/waiting-queue-test.js
```

---

### 3.3 [시나리오 3] 실시간 웹 대시보드로 시각화하여 실행하기 (강력 추천!)
k6 내장 웹 대시보드를 띄워 웹 브라우저에서 초당 요청 수(RPS)와 응답 지연 시간 그래프를 실시간으로 확인합니다.

```powershell
# 브라우저 자동 오픈과 함께 20초간 지속 부하 실행
k6 run --out web-dashboard=open=true k6/dashboard-test.js

# 테스트 완료 후 HTML 리포트로 영구 보관
k6 run --out web-dashboard=export=report.html k6/seat-distributed-lock-test.js
```

---

## 4. 테스트 결과 검증 기준 (Thresholds)

* **정합성 100%**: 동일 좌석에 대해 2건 이상 200 OK(더블 부킹)가 절대 나오지 않아야 합니다.
* **무중단 가용성**: DB 락 지연으로 인한 500 에러가 0건이어야 합니다.
