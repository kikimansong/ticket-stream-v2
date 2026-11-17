-- ========================================================
-- seat-service 전용 스크립트 (ticket_v2_seat_db에 연결 후 실행)
-- ========================================================

-- 1. 좌석 테이블 생성
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE' NOT NULL,
    price BIGINT DEFAULT 110000 NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_seats_concert_status ON seats(concert_id, status);
CREATE INDEX IF NOT EXISTS idx_seats_concert_number ON seats(concert_id, seat_number);

-- 2. 500개 좌석 대량 등록 (VIP 100석, R 150석, S 250석)
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
