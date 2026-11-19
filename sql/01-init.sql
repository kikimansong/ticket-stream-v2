-- ========================================================
-- Ticket Stream v2 - Docker Entrypoint Init Script
-- PostgreSQL 컨테이너 최초 기동 시 단일 DB(ticket_v2_db) 자동 세팅
-- ========================================================

-- 1. concerts (공연 마스터 테이블)
CREATE TABLE IF NOT EXISTS concerts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    concert_date TIMESTAMP NOT NULL,
    total_seats INT DEFAULT 500 NOT NULL
);

-- 2. seats (좌석 테이블)
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE' NOT NULL,
    price BIGINT DEFAULT 110000 NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_seats_concert FOREIGN KEY (concert_id) REFERENCES concerts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_seats_concert_status ON seats(concert_id, status);
CREATE INDEX IF NOT EXISTS idx_seats_concert_number ON seats(concert_id, seat_number);

-- 3. orders (주문 테이블)
CREATE TABLE IF NOT EXISTS orders (
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

CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_seat ON orders(seat_id);

-- 4. payments (결제 테이블)
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    pg_tid VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    approved_at TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_order ON payments(order_id);

-- 5. 기본 데이터 삽입
INSERT INTO concerts (id, title, venue, concert_date, total_seats) VALUES
(1, '2026 WORLD TOUR FINALE IN SEOUL', '올림픽공원 KSPO DOME', CURRENT_TIMESTAMP + INTERVAL '30 days', 500),
(2, '2026 메가 스타디움 라이브 콘서트', '고양종합운동장 주경기장', CURRENT_TIMESTAMP + INTERVAL '45 days', 500)
ON CONFLICT (id) DO NOTHING;

SELECT setval('concerts_id_seq', COALESCE((SELECT MAX(id) FROM concerts), 1));

-- 6. 1번 콘서트 500석 초기 생성
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM seats WHERE concert_id = 1) THEN
        FOR i IN 1..500 LOOP
            INSERT INTO seats (concert_id, seat_number, status, price, version)
            VALUES (
                1,
                CASE 
                    WHEN i <= 100 THEN 'VIP-' || i
                    WHEN i <= 250 THEN 'R-' || (i - 100)
                    ELSE 'S-' || (i - 250)
                END,
                'AVAILABLE',
                CASE 
                    WHEN i <= 100 THEN 150000
                    WHEN i <= 250 THEN 120000
                    ELSE 90000
                END,
                0
            );
        END LOOP;
    END IF;
END $$;
