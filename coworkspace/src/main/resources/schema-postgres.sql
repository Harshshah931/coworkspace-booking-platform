-- PostgreSQL schema reference for CoWorkSpace.
-- CREATE TABLE IF NOT EXISTS statements are safe to re-run.

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(50) UNIQUE,
    phone_verified BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS spaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    price_per_day NUMERIC(19, 2) NOT NULL,
    price_per_month NUMERIC(19, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS seat_units (
    id BIGSERIAL PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    price_per_day NUMERIC(19, 2),
    price_per_month NUMERIC(19, 2),
    space_id BIGINT NOT NULL REFERENCES spaces (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    seat_unit_id BIGINT NOT NULL REFERENCES seat_units (id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    booking_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL,
    CONSTRAINT chk_booking_dates CHECK (end_date > start_date)
);

CREATE INDEX IF NOT EXISTS idx_bookings_seat_dates ON bookings (seat_unit_id, start_date, end_date, status);
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings (user_id);
