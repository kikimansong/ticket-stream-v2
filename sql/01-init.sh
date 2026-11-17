#!/bin/bash
set -e

echo "=========================================================="
echo " [Step 1/5] Creating 4 Microservice Databases..."
echo "=========================================================="
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE ticket_v2_concert_db;
    CREATE DATABASE ticket_v2_seat_db;
    CREATE DATABASE ticket_v2_order_db;
    CREATE DATABASE ticket_v2_payment_db;

    GRANT ALL PRIVILEGES ON DATABASE ticket_v2_concert_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE ticket_v2_seat_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE ticket_v2_order_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE ticket_v2_payment_db TO $POSTGRES_USER;
EOSQL

echo "=========================================================="
echo " [Step 2/5] Initializing concert-service database..."
echo "=========================================================="
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "ticket_v2_concert_db" <<-EOSQL
    CREATE TABLE IF NOT EXISTS concerts (
        id BIGSERIAL PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        venue VARCHAR(255),
        concert_date TIMESTAMP,
        total_seats INT NOT NULL
    );

    INSERT INTO concerts (id, title, venue, concert_date, total_seats)
    VALUES (1, '2026 WORLD TOUR FINALE IN SEOUL', '올림픽공원 KSPO DOME', '2026-12-25 19:00:00', 500)
    ON CONFLICT (id) DO NOTHING;
EOSQL

echo "=========================================================="
echo " [Step 3/5] Initializing seat-service database (500 seats)..."
echo "=========================================================="
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "ticket_v2_seat_db" <<-EOSQL
    CREATE TABLE IF NOT EXISTS seats (
        id BIGSERIAL PRIMARY KEY,
        concert_id BIGINT NOT NULL,
        seat_number VARCHAR(50) NOT NULL,
        status VARCHAR(50) NOT NULL,
        price BIGINT NOT NULL,
        version BIGINT NOT NULL DEFAULT 0
    );

    DO \$\$
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
    END \$\$;
EOSQL

echo "=========================================================="
echo " [Step 4/5] Initializing order-service database..."
echo "=========================================================="
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "ticket_v2_order_db" <<-EOSQL
    CREATE TABLE IF NOT EXISTS orders (
        id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        concert_id BIGINT NOT NULL,
        seat_id BIGINT NOT NULL,
        amount BIGINT NOT NULL,
        status VARCHAR(50) NOT NULL,
        created_at TIMESTAMP
    );
EOSQL

echo "=========================================================="
echo " [Step 5/5] Initializing payment-service database..."
echo "=========================================================="
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "ticket_v2_payment_db" <<-EOSQL
    CREATE TABLE IF NOT EXISTS payments (
        id BIGSERIAL PRIMARY KEY,
        order_id BIGINT NOT NULL,
        seat_id BIGINT NOT NULL,
        amount BIGINT NOT NULL,
        pg_tid VARCHAR(100),
        status VARCHAR(50) NOT NULL,
        approved_at TIMESTAMP
    );
EOSQL

echo "=========================================================="
echo " 🎉 All 4 Databases & 500 Seats Initialized Successfully!"
echo "=========================================================="
