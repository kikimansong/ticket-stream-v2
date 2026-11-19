# 🚀 Ticket Stream v2 단계별 구현 로드맵 (1~5단계)

본 문서는 `ticket-stream-v2` 프로젝트의 점진적 개발 로드맵입니다.  
가장 단순한 단일 조회 기능부터 시작하여 카프카 이벤트 스트리밍, 사가(Saga) 보상 트랜잭션, 그리고 최종 대규모 트래픽 방어용 Redis 대기열까지 단계별로 검증하며 완성해 나갑니다.

---

## 🗺️ 전체 로드맵 요약

```
[1단계: 기초 조회] ──> [2단계: 좌석 선점] ──> [3단계: 첫 카프카 연동] ──> [4단계: 결제 & 사가 패턴] ──> [5단계: 레디스 대기열]
 (콘서트/좌석 목록)      (좌석 상태 변경)        (좌석선점 ➡️ 가주문생성)       (결제완료 ➡️ 좌석SOLD)          (마지막 문지기 부착)
      [완료]               [예정]                   [예정]                     [예정]                     [예정]
```

---

## 📌 1단계: 기초 조회 기능 완성 (기본 뼈대 확인) ✅ 완료

카프카 없이 순수 JPA와 DB 연동이 정상 동작하는지 확인하는 기초 단계입니다.

- **구현 대상**:
  - `domain/concert`: `ConcertRepository`, `ConcertService`, `ConcertController`
  - `domain/seat`: `SeatRepository`, `SeatService`, `SeatController` (목록 조회)
- **엔드포인트 API**:
  - `GET /api/v2/concerts` (전체 공연 목록 조회)
  - `GET /api/v2/seats?concertId={id}` (해당 공연의 500개 좌석 실시간 상태 조회)
- **핵심 로직**:
  - DB 초기 시드 데이터(공연 2건, 좌석 500개)를 JPA 엔티티로 매핑하여 DTO 변환 후 반환.
- **검증 방법**:
  - 브라우저, cURL 또는 Postman 호출:
    ```bash
    curl http://localhost:8080/api/v2/concerts
    curl http://localhost:8080/api/v2/seats?concertId=1
    ```
  - `ApiResponse` 공통 포맷으로 JSON 데이터가 정상 반환되는지 확인.

---

## 📌 2단계: 좌석 임시 선점 (단일 트랜잭션 기능)

사용자가 원하는 좌석을 선택하여 `RESERVED` 상태로 변경하는 핵심 비즈니스 로직입니다.

- **구현 대상**:
  - `domain/seat`: `SeatService.java`, `SeatController.java`
  - `domain/seat/SeatResponse.java` (선점 응답용 DTO)
- **엔드포인트 API**:
  - `POST /api/v2/seats/reserve`
  - **요청 Body**:
    ```json
    {
      "concertId": 1,
      "seatId": 10,
      "userId": 1001
    }
    ```
- **구현 로직**:
  1. `seatRepository.findById(seatId)`로 좌석 조회.
  2. 현재 상태가 `AVAILABLE`인지 검증 (이미 선점/판매된 경우 예외 발생 및 409 Conflict 반환).
  3. `seat.reserve()` 도메인 메서드 호출 (상태를 `RESERVED`로 변경).
  4. 트랜잭션 커밋 및 선점 결과 반환.
- **검증 방법**:
  - `POST /api/v2/seats/reserve` 요청 후 200 OK 응답 확인.
  - `GET /api/v2/seats?concertId=1` 재조회 시 해당 좌석의 `status`가 `RESERVED`로 변경되었는지 확인.
  - 동일 좌석에 대해 다시 선점 요청 시 충돌(예외) 처리 확인.

---

## 📌 3단계: 첫 Kafka 도입! (좌석 선점 ➡️ 가주문 비동기 생성)

좌석 선점이 성공하면 Kafka 이벤트를 발행하여 주문 도메인에서 비동기로 가주문을 자동 생성합니다.

- **구현 대상**:
  - `global/event/SeatReservedEvent.java` (이벤트 DTO)
  - `domain/seat/SeatEventProducer.java` (KafkaTemplate 이벤트 발행)
  - `domain/order/OrderEventConsumer.java` (`@KafkaListener` 수신)
  - `domain/order/OrderService.java` (가주문 `PENDING` 저장)
- **이벤트 스트리밍 흐름**:
  ```
  [SeatService.reserve()] ──(선점 성공)──> [SeatEventProducer]
                                                    │
                                         (Topic: seat.reserved)
                                                    │
                                                    ▼
                                          [OrderEventConsumer]
                                                    │
                                         [OrderService.createOrder()]
                                                    │
                                                    ▼
                                          orders 테이블 저장 (PENDING)
  ```
- **핵심 로직**:
  1. `SeatService.reserve()` 성공 직후 `seatProducer.publishSeatReserved(...)` 호출.
  2. Kafka 토픽 `seat.reserved`로 메시지 전송.
  3. `OrderEventConsumer`가 메시지를 컨슘하여 `orderService.createPendingOrder(...)` 실행.
  4. `orders` 테이블에 주문 레코드 생성 (상태: `PENDING`).
- **검증 방법**:
  - 좌석 선점 API 호출 후 **Kafka UI (`http://localhost:8085`)** 접속 ➡️ `seat.reserved` 토픽에 메시지 적재 확인.
  - DB 또는 `GET /api/v2/orders?userId=1001` 조회 시 `status: PENDING` 상태의 가주문이 자동 생성되었는지 확인.

---

## 📌 4단계: 결제 승인 및 Saga 보상 트랜잭션 (카프카 파이프라인 완성)

가주문에 대한 결제를 진행하고, 결제 성공/실패 결과에 따라 주문과 좌석 상태를 동기화(확정/롤백)합니다.

- **구현 대상**:
  - `domain/payment`: `PaymentService`, `PaymentController`
  - `domain/payment/PaymentEventProducer.java`
  - `domain/order/OrderEventConsumer.java`, `domain/seat/SeatEventConsumer.java`
  - `global/event`: `PaymentCompletedEvent.java`, `PaymentFailedEvent.java`
- **엔드포인트 API**:
  - `POST /api/v2/payments`
  - **요청 Body**:
    ```json
    {
      "orderId": 1,
      "amount": 50000,
      "paymentMethod": "CARD"
    }
    ```
- **시나리오별 흐름**:
  - **[시나리오 A: 결제 성공 시]**:
    1. `payments` 테이블에 승인 내역 저장 (`status = SUCCESS`).
    2. Kafka 토픽 `payment.completed`로 이벤트 발행.
    3. `OrderEventConsumer` 수신 ➡️ 주문 상태를 `COMPLETED`로 변경.
    4. `SeatEventConsumer` 수신 ➡️ 좌석 상태를 `SOLD`(판매완료)로 최종 확정.
  - **[시나리오 B: 결제 실패/타임아웃 시 (보상 트랜잭션)]**:
    1. Kafka 토픽 `payment.failed`로 이벤트 발행.
    2. `OrderEventConsumer` 수신 ➡️ 주문 상태를 `CANCELLED`로 변경.
    3. `SeatEventConsumer` 수신 ➡️ 좌석 상태를 다시 `AVAILABLE`로 롤백 (임시 선점 해제).
- **검증 방법**:
  - 정상 결제 시 `orders.status = COMPLETED`, `seats.status = SOLD` 확인.
  - 결제 실패 시 주문 취소 및 좌석 원복(AVAILABLE) 확인.
  - Kafka UI에서 `payment.completed` 및 `payment.failed` 토픽 컨슈밍 상태 모니터링.

---

## 📌 5단계: Redis 대기열 부착 (최종 완성)

모든 핵심 예매/결제 흐름이 완벽히 검증된 후, 대규모 트래픽 폭주 시 시스템을 보호하는 문지기 역할을 구축합니다.

- **구현 대상**:
  - `domain/queue/QueueService.java` (Redis ZSet 대기열 등록, 순번 조회, 통과 토큰 발급)
  - `domain/queue/QueueScheduler.java` (스케줄러: 초당 N명씩 입장 허용 처리)
  - `domain/queue/QueueController.java` (`POST /api/v2/queue/tokens`, `GET /api/v2/queue/status`)
  - `SeatController.java` (좌석 선점 진입 시 Queue Token 유효성 검증 인터셉터/로직 추가)
- **엔드포인트 API**:
  - `POST /api/v2/queue/tokens` (대기열 진입 및 순번표 발급)
  - `GET /api/v2/queue/status` (현재 내 순번 및 입장 허용 여부 폴링)
- **구현 로직**:
  1. 클라이언트가 대기열 진입 요청 ➡️ Redis ZSet에 `(timestamp, userId)` 등록.
  2. 사용자는 본인 순번을 주기적으로 폴링.
  3. `QueueScheduler`가 주기적으로 상위 N명을 대기열에서 활성 상태(Active Set/Key)로 승격.
  4. 입장 순번이 된 사용자는 유효 토큰(Active Token)을 획득.
  5. 좌석 선점(`POST /api/v2/seats/reserve`) 요청 헤더/파라미터의 토큰을 검증하여 통과자만 선점 허용.
- **검증 방법**:
  - k6 스크립트(`k6/waiting-queue-test.js`)를 통해 동시 수백 명 부하 테스트 수행.
  - Redis 대기열을 거쳐 순차적으로 좌석 선점으로 진입하는지 확인.
