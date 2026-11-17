-- ========================================================
-- Ticket Stream v2 - DDL (PostgreSQL)
-- MSA 아키텍처 (Database-per-Service 4개 전용 DB 스키마 정의)
-- ========================================================

-- [1] 4개 마이크로서비스 전용 데이터베이스 생성 (기본 postgres/main_db에서 실행)
CREATE DATABASE ticket_v2_concert_db;
CREATE DATABASE ticket_v2_seat_db;
CREATE DATABASE ticket_v2_order_db;
CREATE DATABASE ticket_v2_payment_db;

GRANT ALL PRIVILEGES ON DATABASE ticket_v2_concert_db TO ticket;
GRANT ALL PRIVILEGES ON DATABASE ticket_v2_seat_db TO ticket;
GRANT ALL PRIVILEGES ON DATABASE ticket_v2_order_db TO ticket;
GRANT ALL PRIVILEGES ON DATABASE ticket_v2_payment_db TO ticket;


-- ========================================================
-- [2] concert-service 스키마 (ticket_v2_concert_db)
-- ========================================================
-- \c ticket_v2_concert_db;

CREATE TABLE IF NOT EXISTS concerts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    concert_date TIMESTAMP NOT NULL,
    total_seats INT DEFAULT 500 NOT NULL
);

COMMENT ON TABLE concerts IS '공연 정보 마스터 테이블 (concert-service)';
COMMENT ON COLUMN concerts.id IS '공연 고유 식별자 (PK)';
COMMENT ON COLUMN concerts.title IS '공연 제목';
COMMENT ON COLUMN concerts.venue IS '공연 장소 (예: KSPO DOME)';
COMMENT ON COLUMN concerts.concert_date IS '공연 일시';
COMMENT ON COLUMN concerts.total_seats IS '총 좌석 수';


-- ========================================================
-- [3] seat-service 스키마 (ticket_v2_seat_db)
-- ========================================================
-- \c ticket_v2_seat_db;

CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE' NOT NULL,
    price BIGINT DEFAULT 110000 NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL
);

COMMENT ON TABLE seats IS '좌석 정보 및 Redisson 분산 락 상태 테이블 (seat-service)';
COMMENT ON COLUMN seats.id IS '좌석 고유 식별자 (PK)';
COMMENT ON COLUMN seats.concert_id IS '해당 공연 ID';
COMMENT ON COLUMN seats.seat_number IS '좌석 번호 (예: VIP-1, R-10, S-50)';
COMMENT ON COLUMN seats.status IS '좌석 상태 (AVAILABLE: 예매가능, RESERVED: 5분 임시선점, SOLD: 판매확정)';
COMMENT ON COLUMN seats.price IS '좌석 티켓 가격';
COMMENT ON COLUMN seats.version IS '버전 번호 (@Version)';

CREATE INDEX IF NOT EXISTS idx_seats_concert_status ON seats(concert_id, status);
CREATE INDEX IF NOT EXISTS idx_seats_concert_number ON seats(concert_id, seat_number);


-- ========================================================
-- [4] order-service 스키마 (ticket_v2_order_db)
-- ========================================================
-- \c ticket_v2_order_db;

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    concert_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE orders IS '예매 주문 및 Saga 라이프사이클 테이블 (order-service)';
COMMENT ON COLUMN orders.id IS '주문 고유 식별자 (PK)';
COMMENT ON COLUMN orders.user_id IS '예매자 사용자 ID';
COMMENT ON COLUMN orders.concert_id IS '예매한 공연 ID';
COMMENT ON COLUMN orders.seat_id IS '선점한 좌석 ID';
COMMENT ON COLUMN orders.amount IS '결제 주문 금액';
COMMENT ON COLUMN orders.status IS '주문 상태 (PENDING: 결제대기, COMPLETED: 완료, CANCELLED: 취소)';
COMMENT ON COLUMN orders.created_at IS '주문 생성 일시';

CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_seat ON orders(seat_id);


-- ========================================================
-- [5] payment-service 스키마 (ticket_v2_payment_db)
-- ========================================================
-- \c ticket_v2_payment_db;

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    pg_tid VARCHAR(100),
    status VARCHAR(50) DEFAULT 'SUCCESS' NOT NULL,
    approved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE payments IS 'PG 결제 승인 원장 테이블 (payment-service)';
COMMENT ON COLUMN payments.id IS '결제 고유 식별자 (PK)';
COMMENT ON COLUMN payments.order_id IS '연계된 주문 ID';
COMMENT ON COLUMN payments.seat_id IS '결제된 좌석 ID';
COMMENT ON COLUMN payments.amount IS '결제 승인 금액';
COMMENT ON COLUMN payments.pg_tid IS 'PG사 고유 거래 번호';
COMMENT ON COLUMN payments.status IS '결제 상태 (SUCCESS, FAILED)';
COMMENT ON COLUMN payments.approved_at IS '결제 승인 일시';

CREATE INDEX IF NOT EXISTS idx_payments_order ON payments(order_id);
