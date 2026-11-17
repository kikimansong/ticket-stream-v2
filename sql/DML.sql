-- ========================================================
-- Ticket Stream v2 - DML (PostgreSQL)
-- 테스트용 기초 데이터 (콘서트 등록 + 500개 좌석 자동 생성)
-- ========================================================

-- ========================================================
-- 1. concert-service 기초 데이터 (ticket_v2_concert_db에 적용)
-- ========================================================
-- \c ticket_v2_concert_db;

INSERT INTO concerts (id, title, venue, concert_date, total_seats) VALUES
(1, '2026 WORLD TOUR FINALE IN SEOUL', '올림픽공원 KSPO DOME', CURRENT_TIMESTAMP + INTERVAL '30 days', 500),
(2, '2026 메가 스타디움 라이브 콘서트', '고양종합운동장 주경기장', CURRENT_TIMESTAMP + INTERVAL '45 days', 500)
ON CONFLICT (id) DO NOTHING;

-- 시퀀스 동기화
SELECT setval('concerts_id_seq', (SELECT MAX(id) FROM concerts));


-- ========================================================
-- 2. seat-service 500석 대량 등록 (ticket_v2_seat_db에 적용)
--    - VIP석: 1~100번 (100석, ₩150,000)
--    - R석  : 1~150번 (150석, ₩120,000)
--    - S석  : 1~250번 (250석, ₩90,000)
--    총 500석 자동 생성
-- ========================================================
-- \c ticket_v2_seat_db;

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


-- ========================================================
-- 3. [참고] 테스트 중 좌석 상태를 다시 초기화하고 싶을 때 실행하는 쿼리
-- ========================================================
-- [ticket_v2_seat_db]
-- UPDATE seats SET status = 'AVAILABLE', version = 0 WHERE concert_id = 1;

-- [ticket_v2_order_db]
-- TRUNCATE TABLE orders RESTART IDENTITY;

-- [ticket_v2_payment_db]
-- TRUNCATE TABLE payments RESTART IDENTITY;
