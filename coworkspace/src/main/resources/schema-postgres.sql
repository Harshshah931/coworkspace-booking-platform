-- PostgreSQL Initial Schema Reference for CoWorkSpace
-- Designed for non-destructive, safe execution (CREATE TABLE IF NOT EXISTS)

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
    space_id BIGINT NOT NULL,
    CONSTRAINT fk_seat_space FOREIGN KEY (space_id) REFERENCES spaces (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    seat_unit_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    booking_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_booking_seat FOREIGN KEY (seat_unit_id) REFERENCES seat_units (id)
);

CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    otp_hmac VARCHAR(64) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    client_ip VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INT DEFAULT 0 NOT NULL,
    consumed BOOLEAN DEFAULT FALSE NOT NULL,
    superseded BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS registration_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    phone VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bookings_seat_dates ON bookings (seat_unit_id, start_date, end_date, status);
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_otp_phone_purpose ON otp_challenges (phone, purpose);
CREATE INDEX IF NOT EXISTS idx_otp_client_ip ON otp_challenges (client_ip);
CREATE INDEX IF NOT EXISTS idx_reg_tokens_token ON registration_tokens (token);
