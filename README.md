# Ticket Stream v2 🎫

v1의 단일 모놀리식 및 단일 DB 환경에서 마주했던 **HikariCP 커넥션 풀 고갈과 DB 락 대기 타임아웃** 한계를 극복하기 위해, **대기열 서비스(Redis ZSet)**, **분산 락(Redisson)**, **서비스별 독립 DB(Database-per-Service)**, 그리고 **Kafka 기반 이벤트 주도(Saga 패턴)** 아키텍처를 도입하여 대규모 트래픽을 안정적으로 처리하는 마이크로서비스 프로젝트입니다.

---

## 1. 프론트, 백 설치 및 실행 방법

### 1.1 사전 요구사항 (Prerequisites)
- **Java**: 21 이상
- **Node.js**: 18.x 이상
- **Docker & Docker Compose**: PostgreSQL, Redis, Kafka(KRaft) 인프라 컨테이너 구동용
- **k6** (선택 사항): 부하 테스트용 CLI (`C:\Program Files\k6\k6.exe` 설치됨)

---

### 1.2 실행 순서

#### Step 1. 인프라 컨테이너 구동 (Docker Compose)
프로젝트 루트 디렉토리에서 PostgreSQL, Redis, Kafka 컨테이너를 실행합니다.
```powershell
docker-compose up -d
```
> - **PostgreSQL**: `localhost:5432` / 계정: `ticket` / 암호: `1234`  
>   *(컨테이너 최초 구동 시 `sql/01-init.sql`에 의해 4개 전용 DB 및 기본 데이터가 자동 세팅됩니다)*
> - **Redis**: `localhost:6379` (대기열 및 분산 락용)
> - **Kafka**: `localhost:9092` (KRaft 브로커 단독 모드)

#### Step 2. 백엔드 마이크로서비스 구동
각각의 새 터미널을 열고 필요한 서비스를 실행합니다.
```powershell
# [Port 8081] 대기열 서비스
.\gradlew.bat :services:waiting-service:bootRun

# [Port 8082] 공연 서비스
.\gradlew.bat :services:concert-service:bootRun

# [Port 8083] 좌석 선점 서비스
.\gradlew.bat :services:seat-service:bootRun

# [Port 8084] 주문 서비스
.\gradlew.bat :services:order-service:bootRun

# [Port 8085] 결제 서비스
.\gradlew.bat :services:payment-service:bootRun
```

#### Step 3. 프론트엔드(Next.js) 실행
새 터미널을 열고 `front` 디렉토리로 이동하여 의존성 설치 및 개발 서버를 실행합니다.
```powershell
cd front
npm install
npm run dev
```
> 브라우저에서 `http://localhost:3000`에 접속하여 실시간 대기열 및 좌석 예매 콘솔을 확인합니다.

#### Step 4. (선택) k6 부하 테스트 실행
백엔드 및 인프라가 켜져 있는 상태에서 대량 동시 요청 부하 테스트를 수행합니다.
```powershell
# 대기열 진입 및 토큰 발급 부하 테스트
k6 run k6/waiting-queue-test.js

# Redisson 분산 락 동시 좌석 선점 테스트
k6 run k6/seat-distributed-lock-test.js
```

---

## 2. 프로젝트 개요

대규모 티켓팅(인터파크, 멜론티켓 등) 환경에서는 수만~수십만 명의 사용자가 단 1개의 좌석을 선점하려 시도하며, v1 프로젝트에서는 이로 인해 **단일 DB 커넥션 풀 고갈(3초 타임아웃)**과 **전체 서비스 마비(Cascading Failure)**가 발생했습니다.

**Ticket Stream v2**는 다음과 같은 아키텍처 전환을 통해 이 문제를 근본적으로 해결했습니다:
- **대기열 서비스(Redis ZSet)**: 대량 트래픽이 백엔드 DB로 직격하지 못하도록 유량을 초당 100~200명 단위로 조절하는 1차 방파제 구축.
- **Redisson 분산 락(Fast-Fail)**: DB `SELECT FOR UPDATE` 대신 인메모리 분산 락을 사용하여 밀리초 단위로 1등만 선점 통과시키고 나머지 동시 요청을 즉각 차단.
- **Database-per-Service**: 서비스별(공연, 좌석, 주문, 결제) 독립 DB를 구성하여 결제나 주문의 부하가 좌석 조회에 영향을 주지 않도록 물리적 격리.
- **Kafka 기반 Saga 패턴**: 동기식 단일 트랜잭션 대신 비동기 이벤트 스트리밍을 통해 서비스 간 결합도를 제거하고 최종 일관성(Eventual Consistency) 및 보상 트랜잭션 보장.

### 기술 스택
- **Backend**: Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Kafka, Redisson 3.34
- **Database & Cache**: PostgreSQL 15, Redis 7 (Sorted Set / Standalone)
- **Message Broker**: Apache Kafka 3.7 (KRaft Mode)
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS
- **Load Test**: Grafana k6

---

## 3. 프로젝트 목표

1. **DB 커넥션 풀 고갈의 원천 차단**: 대기열 서비스를 통해 DB가 수용 가능한 수준으로만 유입을 제한하여 타임아웃(500 에러) 0건 달성.
2. **초고속 좌석 임시 선점 (5분 TTL)**: Redisson 분산 락을 통해 RDB 락 점유 시간을 0으로 줄이고, 5분 내 미결제 시 자동 롤백.
3. **완전한 서비스 및 장애 격리(Fault Isolation)**: 결제 PG사 지연이나 주문 서버 부하가 발생해도 좌석 조회/선점 서비스는 100% 정상 작동.
4. **Kafka 기반 Saga 보상 트랜잭션 검증**: 결제 실패(`payment.failed`) 발생 시 Kafka 이벤트를 통해 주문 취소 및 좌석 선점 해제(`AVAILABLE`) 자동 복원.

---

## 4. API 명세서

모든 API는 서비스별 전용 포트와 `http://localhost:{port}/api/v1` 베이스 경로를 사용하며 공통 응답 규격(`ApiResponse<T>`)을 반환합니다.

### 4.1 서비스별 엔드포인트 목록

| 서비스 | Port | HTTP Method | Endpoint URI | 설명 |
| :--- | :---: | :---: | :--- | :--- |
| **대기열** | `8081` | `POST` | `/api/v1/queue/tokens` | 대기열 진입 및 고유 대기 토큰 발급 |
| **대기열** | `8081` | `GET` | `/api/v1/queue/status` | 내 현재 순번 및 입장 가능 여부 폴링 |
| **공연** | `8082` | `GET` | `/api/v1/concerts` | 전체 공연 목록 조회 |
| **공연** | `8082` | `GET` | `/api/v1/concerts/{id}` | 특정 공연 상세 정보 조회 |
| **좌석** | `8083` | `GET` | `/api/v1/seats?concertId={id}` | 500개 좌석 실시간 상태 조회 |
| **좌석** | `8083` | `POST` | `/api/v1/seats/reserve` | **좌석 임시 선점** (Redisson 분산 락, 5분 유효) |
| **좌석** | `8083` | `POST` | `/api/v1/seats/reset/{concertId}` | 부하 테스트용 좌석 전체 리셋 (`AVAILABLE`) |
| **주문** | `8084` | `GET` | `/api/v1/orders/{id}` | 주문 상세 정보 단건 조회 |
| **주문** | `8084` | `GET` | `/api/v1/orders?userId={userId}` | 특정 사용자의 주문 목록 조회 |
| **결제** | `8085` | `POST` | `/api/v1/payments` | **결제 승인 처리** (외부 PG 연동 및 이벤트 발행) |
| **결제** | `8085` | `GET` | `/api/v1/payments/{orderId}` | 특정 주문의 결제 승인 내역 조회 |

---

### 4.2 주요 API 상세 스펙

#### 1. [POST] `/api/v1/seats/reserve` - 좌석 임시 선점 (Port 8083)
- **Request Body**:
  ```json
  {
    "userId": 1001,
    "concertId": 1,
    "seatId": 1
  }
  ```
- **Response Body (200 OK - 선점 성공 시)**:
  ```json
  {
    "success": true,
    "message": "좌석 임시 선점 성공",
    "data": {
      "seatId": 1,
      "concertId": 1,
      "userId": 1001,
      "seatNumber": "VIP-1",
      "status": "RESERVED",
      "price": 150000,
      "reservedAt": "2026-11-17T17:30:00",
      "message": "좌석이 임시 선점되었습니다. 5분 내에 결제를 완료해주세요."
    }
  }
  ```
- **HTTP 응답 상태 코드**:
  - `200 OK`: 좌석 임시 선점 성공 (Kafka로 `seat.reserved` 이벤트 자동 발행)
  - `409 Conflict`: 이미 다른 사용자가 선점 중이거나 판매 완료된 좌석
  - `400 Bad Request`: 존재하지 않는 좌석/공연 식별자

#### 2. [POST] `/api/v1/payments` - 결제 승인 요청 (Port 8085)
- **Request Body**:
  ```json
  {
    "orderId": 1,
    "seatId": 1,
    "amount": 150000,
    "paymentMethod": "CARD"
  }
  ```
- **Response Body (200 OK - 결제 성공 시)**:
  ```json
  {
    "success": true,
    "message": "결제 성공",
    "data": {
      "paymentId": 1,
      "orderId": 1,
      "seatId": 1,
      "amount": 150000,
      "pgTid": "PG_a1b2c3d4",
      "status": "SUCCESS",
      "approvedAt": "2026-11-17T17:31:00",
      "message": "결제가 정상적으로 승인되었습니다."
    }
  }
  ```
> **비고**: 결제 완료 시 Kafka로 `payment.completed` 이벤트가 발행되어, `order-service`는 주문 완료(`COMPLETED`), `seat-service`는 좌석 판매 확정(`SOLD`)으로 비동기 동기화됩니다.

---

## 5. 프로세스 및 플로우 (API 명 포함)

### 5.1 전체 예매 라이프사이클 흐름 (Saga 패턴)

```
[1. 대기열 진입]
POST /api/v1/queue/tokens
GET  /api/v1/queue/status (순번 폴링)
      │
      ▼ (내 차례 도달 -> 입장 허용)
[2. 좌석 선점]
POST /api/v1/seats/reserve (Port 8083)
      │ • Redisson 분산 락 획득 (5분 TTL)
      │ • seats.status = RESERVED
      │ • [Kafka 발행] Topic: seat.reserved
      ▼
[3. 가주문 자동 생성] (order-service가 구독)
      │ • orders.status = PENDING
      │ • [Kafka 발행] Topic: order.created
      ▼
[4. 결제 승인 요청]
POST /api/v1/payments (Port 8085)
      │ • PG 승인 완료 (payments.status = SUCCESS)
      │
      ├── [정상 완료] [Kafka 발행] Topic: payment.completed
      │     ├── order-service 구독 -> orders.status = COMPLETED
      │     └── seat-service  구독 -> seats.status  = SOLD & 분산 락 해제
      │
      └── [결제 실패] [Kafka 발행] Topic: payment.failed (보상 트랜잭션)
            ├── order-service 구독 -> orders.status = CANCELLED
            └── seat-service  구독 -> seats.status  = AVAILABLE 롤백 & 분산 락 해제
```

---

### 5.2 장애 및 보상 트랜잭션(Compensating Transaction) 비교

| 상황 | 발생 원인 | Saga 보상 트랜잭션 흐름 | 결과 상태 |
| :--- | :--- | :--- | :--- |
| **정상 결제** | 5분 내 결제 승인 성공 | `payment.completed` 이벤트 전파 | 좌석 `SOLD`, 주문 `COMPLETED` |
| **결제 실패** | 카드 한도 초과, 잔액 부족 | `payment.failed` 이벤트 전파 $\rightarrow$ 좌석 선점 즉시 취소 | 좌석 `AVAILABLE` 복원, 주문 `CANCELLED` |
| **선점 타임아웃** | 5분 동안 결제 미진행 | Redisson 락 TTL(300초) 만료 및 선점 해제 | 좌석 `AVAILABLE` 자동 전환, 타 사용자 예매 가능 |

---

## 6. 프로젝트 결과: v1 vs v2 아키텍처 비교

### 6.1 핵심 성능 및 안정성 비교

| 평가 항목 | v1 (단일 모놀리스 + 단일 RDB) | v2 (MSA + Redis + Kafka) | 개선 결과 |
| :--- | :--- | :--- | :---: |
| **동시성 제어 방식** | DB Row Lock (`SELECT FOR UPDATE`) | **Redisson 분산 락 (In-Memory)** | RDB 락 대기 시간 제거 |
| **100명 이상 폭주 시** | **HikariCP 고갈 및 3초 타임아웃 (500 에러)** | **대기열 큐 완충 및 0건 타임아웃** | 100% 가용성 유지 |
| **트랜잭션 범위** | 선점 + 가주문 + 결제까지 단일 트랜잭션 | **선점 / 주문 / 결제 분리 (Saga)** | DB 트랜잭션 점유 시간 최소화 |
| **장애 격리 (Isolation)**| 결제 지연 시 전체 좌석 조회까지 마비 | **결제 서버가 다운되어도 좌석 조회/선점 정상** | 장애 전파 원천 차단 |
| **스케일아웃(확장성)** | 전체 시스템 통째로 확장 (DB가 병목) | **대기열 및 좌석 서비스만 독립 스케일아웃** | 인프라 비용 절감 및 효율화 |
