-- ========================================================
-- Ticket Stream v2 - Monolithic DDL (PostgreSQL)
-- 모놀리식 단일 데이터베이스(ticket_v2_db) 스키마 정의
-- ========================================================

-- 1. 기존 테이블 정리 (역순 삭제)
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS concerts CASCADE;

-- 2. concerts (공연 마스터 테이블)
CREATE TABLE concerts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    concert_date TIMESTAMP NOT NULL,
    total_seats INT DEFAULT 500 NOT NULL
);

COMMENT ON TABLE concerts IS '공연 정보 마스터 테이블';

-- 3. seats (좌석 테이블 - Redisson 분산 락 및 선점 대상)
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE' NOT NULL,
    price BIGINT DEFAULT 110000 NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_seats_concert FOREIGN KEY (concert_id) REFERENCES concerts(id) ON DELETE CASCADE
);

COMMENT ON TABLE seats IS '좌석 정보 및 선점 관리 테이블';
CREATE INDEX idx_seats_concert_status ON seats(concert_id, status);
CREATE INDEX idx_seats_concert_number ON seats(concert_id, seat_number);

-- 4. orders (주문 테이블)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    concert_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_concert FOREIGN KEY (concert_id) REFERENCES concerts(id),
    CONSTRAINT fk_orders_seat FOREIGN KEY (seat_id) REFERENCES seats(id)
);

COMMENT ON TABLE orders IS '주문 관리 테이블';
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_seat ON orders(seat_id);

-- 5. payments (결제 테이블)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    pg_tid VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    approved_at TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

COMMENT ON TABLE payments IS '결제 내역 관리 테이블';
CREATE INDEX idx_payments_order ON payments(order_id);
