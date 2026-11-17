-- ========================================================
-- concert-service 전용 스크립트 (ticket_v2_concert_db에 연결 후 실행)
-- ========================================================

CREATE TABLE IF NOT EXISTS concerts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    concert_date TIMESTAMP NOT NULL,
    total_seats INT DEFAULT 500 NOT NULL
);

INSERT INTO concerts (id, title, venue, concert_date, total_seats) VALUES
(1, '2026 WORLD TOUR FINALE IN SEOUL', '올림픽공원 KSPO DOME', CURRENT_TIMESTAMP + INTERVAL '30 days', 500),
(2, '2026 메가 스타디움 라이브 콘서트', '고양종합운동장 주경기장', CURRENT_TIMESTAMP + INTERVAL '45 days', 500)
ON CONFLICT (id) DO NOTHING;
