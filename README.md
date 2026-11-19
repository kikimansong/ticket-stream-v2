# Ticket Stream v2 🎫

**Ticket Stream v2**는 v1의 단일 모놀리식 구조의 단순성과 생산성을 유지하면서, 대규모 트래픽 처리를 위해 **Redis(대기열 유량 제어)**와 **Apache Kafka(이벤트 스트리밍)**를 도입한 **이벤트 주도 모놀리식(Event-Driven Monolith) 티켓 예매 시스템**입니다.

> 📖 **단계별 개발 로드맵**: [docs/implementation-roadmap.md](file:///d:/Projects/ticket-stream-v2/docs/implementation-roadmap.md)에서 1단계부터 5단계까지의 점진적 구현 계획을 확인하실 수 있습니다.

---

## 1. 프론트, 백 설치 및 실행 방법

### 1.1 사전 요구사항 (Prerequisites)
- **Java**: 21 이상
- **Node.js**: 18.x 이상
- **Docker & Docker Compose**: PostgreSQL, Redis, Kafka(KRaft), Kafka UI 인프라 컨테이너 구동용
- **k6** (선택 사항): 부하 테스트용 CLI (`C:\Program Files\k6\k6.exe` 설치됨)

---

### 1.2 실행 순서

#### Step 1. 인프라 컨테이너 구동 (Docker Compose)
프로젝트 루트 디렉토리에서 인프라 컨테이너(PostgreSQL, Redis, Kafka, Kafka UI)를 실행합니다.
```powershell
docker-compose up -d
```
> - **PostgreSQL**: `localhost:5432` / DB: `ticket_v2_db` / 계정: `ticket` / 암호: `1234`  
>   *(컨테이너 최초 구동 시 `sql/01-init.sql`에 의해 콘서트 및 500개 좌석 데이터가 자동 세팅됩니다)*
> - **Redis**: `localhost:6379` (대기열 ZSet 및 활성 토큰 관리용)
> - **Kafka**: `localhost:9092` (KRaft 브로커 단독 모드)
> - **Kafka UI**: `http://localhost:8085` (웹 브라우저 토픽/메시지 모니터링 콘솔)

#### Step 2. 백엔드(Spring Boot 모놀리식) 구동
단일 Spring Boot 애플리케이션으로 실행합니다. (기본 포트: **8080**)
```powershell
.\gradlew.bat bootRun
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
```

---

## 2. 프로젝트 아키텍처 및 특징

대규모 티켓팅 환경에서는 수만~수십만 명의 동시 접속자가 몰려 **단일 DB 커넥션 풀 고갈(HikariCP 타임아웃)**이 발생합니다.  
v2에서는 모놀리식의 복잡성을 낮추면서 인프라 레벨의 완충과 비동기 처리를 도입했습니다.

- **Redis 대기열(Virtual Waiting Room)**: 대량 트래픽이 DB로 직격하지 못하도록 Redis ZSet으로 순번을 매기고, 초당 수용 가능한 유량(예: 50~100명)으로만 예매 권한 토큰을 발급하여 DB 커넥션 풀을 완벽하게 보호합니다.
- **Apache Kafka 이벤트 스트리밍**: 좌석 선점, 주문 생성, 결제 처리 간의 긴 트랜잭션을 동기식으로 묶지 않고, 비동기 이벤트 스트리밍으로 결합도를 분리하여 빠른 응답성과 최종 일관성(Eventual Consistency)을 보장합니다.
- **단일 모놀리식 DB (`ticket_v2_db`)**: 마이크로서비스의 분할 DB 복잡성을 걷어내고, v1과 동일하게 단일 데이터베이스 환경에서 `concerts`, `seats`, `orders`, `payments`를 관리합니다.

### 기술 스택
- **Backend**: Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Kafka, Spring Data Redis
- **Database & Cache**: PostgreSQL 15, Redis 7 (Sorted Set / Standalone)
- **Message Broker**: Apache Kafka 3.7 (KRaft Mode)
- **Monitoring UI**: Kafka UI (`http://localhost:8085`)
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS
- **Load Test**: Grafana k6

---

## 3. 디렉토리 구조 (v1 모놀리식 기반)

```
ticket-stream-v2/
├── src/main/
│   ├── java/com/ticketstream/
│   │   ├── TicketStreamApplication.java               # 단일 Spring Boot 메인 클래스
│   │   ├── domain/                                    # 도메인별 패키지 (응집도 높은 Feature-by-Domain)
│   │   │   ├── concert/                               # Concert(엔티티), Controller, Service, Repo, Req/Res
│   │   │   ├── seat/                                  # Seat, Controller, Service, Repo, Req/Res, Producer, Consumer
│   │   │   ├── order/                                 # Order, Controller, Service, Repo, Req/Res, Producer, Consumer
│   │   │   ├── payment/                               # Payment, Controller, Service, Repo, Req/Res, Producer
│   │   │   └── queue/                                 # QueueController, QueueService, QueueScheduler, Req/Res
│   │   └── global/                                    # 공통 인프라/설정
│   │       ├── config/                                # KafkaConfig, RedisConfig, WebConfig
│   │       ├── dto/                                   # ApiResponse
│   │       ├── enums/                                 # OrderStatus, PaymentStatus, SeatStatus
│   │       ├── event/                                 # OrderCreatedEvent, PaymentCompletedEvent 등
│   │       └── exception/                             # GlobalExceptionHandler
│   └── resources/
│       └── application.yml                            # 단일 8080 포트 및 DB/Redis/Kafka 설정
├── sql/
│   ├── 01-init.sql                                    # 컨테이너 기동 시 단일 DB 자동 초기화 스크립트
│   ├── DDL.sql                                        # 전체 테이블/인덱스 DDL 명세
│   └── DML.sql                                        # 콘서트/500석 초기 데이터 DML 명세
├── docker-compose.yml                                 # PostgreSQL, Redis, Kafka, Kafka UI
├── build.gradle                                       # 단일 프로젝트 빌드 설정
└── settings.gradle                                    # rootProject.name = 'ticket-stream-v2'
```

---

## 4. API 명세서 (단일 포트 8080)

모든 API는 단일 포트 `http://localhost:8080/api/v2` 베이스 경로를 사용하며 공통 응답 규격(`ApiResponse<T>`)을 반환합니다.

### 4.1 엔드포인트 목록

| 도메인 | HTTP Method | Endpoint URI | 설명 |
| :--- | :---: | :--- | :--- |
| **대기열** | `POST` | `/api/v2/queue/tokens` | 대기열 진입 및 고유 대기 순번 발급 |
| **대기열** | `GET` | `/api/v2/queue/status` | 내 현재 순번 및 예매 입장 가능 여부 폴링 |
| **공연** | `GET` | `/api/v2/concerts` | 전체 공연 목록 조회 |
| **공연** | `GET` | `/api/v2/concerts/{id}` | 특정 공연 상세 정보 조회 |
| **좌석** | `GET` | `/api/v2/seats?concertId={id}` | 해당 공연의 500개 좌석 실시간 상태 조회 |
| **좌석** | `POST` | `/api/v2/seats/reserve` | 좌석 임시 선점 요청 |
| **좌석** | `POST` | `/api/v2/seats/reset/{concertId}` | 부하 테스트용 좌석 전체 리셋 (`AVAILABLE`) |
| **주문** | `GET` | `/api/v2/orders/{id}` | 주문 상세 정보 단건 조회 |
| **주문** | `GET` | `/api/v2/orders?userId={userId}` | 특정 사용자의 주문 목록 조회 |
| **결제** | `POST` | `/api/v2/payments` | 결제 승인 요청 (Kafka 이벤트 발행) |
| **결제** | `GET` | `/api/v2/payments/{orderId}` | 특정 주문의 결제 승인 내역 조회 |

---

## 5. 이벤트 스트리밍 흐름 (Kafka 연계)

```
[1. 대기열 진입]
POST /api/v2/queue/tokens
GET  /api/v2/queue/status (순번 폴링 -> 순번 도달 시 유효 토큰 발급)
      │
      ▼ (예매 페이지 입장)
[2. 좌석 선점]
POST /api/v2/seats/reserve
      │ • seats.status = RESERVED
      │ • [Kafka 발행] Topic: seat.reserved
      ▼
[3. 가주문 자동 생성] (OrderEventConsumer 구독)
      │ • orders.status = PENDING
      │ • [Kafka 발행] Topic: order.created
      ▼
[4. 결제 승인 처리]
POST /api/v2/payments
      │ • PG 승인 완료 (payments.status = SUCCESS)
      │
      ├── [정상 완료] [Kafka 발행] Topic: payment.completed
      │     ├── OrderEventConsumer 구독 -> orders.status = COMPLETED
      │     └── SeatEventConsumer  구독 -> seats.status  = SOLD
      │
      └── [결제 실패] [Kafka 발행] Topic: payment.failed (보상 트랜잭션)
            ├── OrderEventConsumer 구독 -> orders.status = CANCELLED
            └── SeatEventConsumer  구독 -> seats.status  = AVAILABLE 롤백
```
