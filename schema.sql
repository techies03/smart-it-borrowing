-- ============================================================
-- Smart IT Equipment Borrowing & Tracking System
-- MySQL Schema  |  Database: smart_it_borrowing
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_it_borrowing
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_it_borrowing;

-- ---------------------------------------------------------------
-- TABLE: users
-- Stores system users (Admin + regular Users)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100)        NOT NULL,
    email        VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255)       NOT NULL,   -- BCrypt hash
    role         ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- TABLE: items
-- IT equipment that can be borrowed
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS items (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(150)  NOT NULL,
    category     VARCHAR(100)  NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    status       ENUM('AVAILABLE','UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    item_condition ENUM('GOOD','DAMAGED')    NOT NULL DEFAULT 'GOOD',
    image_url    VARCHAR(500)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- TABLE: bookings
-- Tracks each borrow request
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    id                 INT PRIMARY KEY AUTO_INCREMENT,
    user_id            INT           NOT NULL,
    item_id            INT           NOT NULL,
    quantity           INT           NOT NULL DEFAULT 1,
    borrow_date        DATETIME      NOT NULL,
    return_date        DATETIME      NOT NULL,          -- expected return
    actual_return_date DATETIME      NULL,              -- NULL until returned
    status             ENUM('PENDING','APPROVED','REJECTED','RETURN_PENDING','RETURNED') NOT NULL DEFAULT 'PENDING',
    penalty            DECIMAL(10,2) NOT NULL DEFAULT 0.00,  -- RM, calculated on return
    condition_before   ENUM('GOOD','DAMAGED') NOT NULL DEFAULT 'GOOD',
    condition_after    ENUM('GOOD','DAMAGED') NULL,          -- NULL until returned
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_booking_item FOREIGN KEY (item_id) REFERENCES items(id)  ON DELETE CASCADE,
    CONSTRAINT chk_dates CHECK (return_date >= borrow_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- INDEXES for performance
-- ---------------------------------------------------------------
CREATE INDEX idx_bookings_item_dates ON bookings (item_id, borrow_date, return_date);
CREATE INDEX idx_bookings_user       ON bookings (user_id);
CREATE INDEX idx_bookings_status     ON bookings (status);
CREATE INDEX idx_items_category      ON items (category);

-- ---------------------------------------------------------------
-- SEED DATA
-- Admin user — password: admin123  (BCrypt hash generated externally)
-- Regular user — password: admin123
-- NOTE: Regenerate BCrypt hashes via SeedData.java if needed
-- ---------------------------------------------------------------
INSERT INTO users (name, email, password_hash, role) VALUES
    ('System Admin',  'admin@smartit.com', '$2a$12$PnRn.8eDoIk76cSJLtdbs.ORy9CWKG1jILmSodPbAojTTCwbC.4fK', 'ADMIN'),
    ('John Doe',      'john@smartit.com',  '$2a$12$PnRn.8eDoIk76cSJLtdbs.ORy9CWKG1jILmSodPbAojTTCwbC.4fK', 'USER');

-- Sample items
INSERT INTO items (name, category, quantity, status, item_condition) VALUES
    ('Dell Laptop XPS 15',     'Laptop',       5, 'AVAILABLE',   'GOOD'),
    ('HP LaserJet Printer',    'Printer',       2, 'AVAILABLE',   'GOOD'),
    ('Logitech Webcam C920',   'Webcam',        8, 'AVAILABLE',   'GOOD'),
    ('HDMI to VGA Adapter',    'Accessory',    10, 'AVAILABLE',   'GOOD'),
    ('Wireless Mouse',         'Accessory',    15, 'AVAILABLE',   'GOOD'),
    ('TP-Link Network Switch', 'Networking',    3, 'AVAILABLE',   'GOOD');

-- ============================================================
-- NOTE on seed passwords:
--   admin@smartit.com → password: admin123
--   john@smartit.com  → password: admin123   (change after first login)
--   Hash above is BCrypt of "admin123" at cost 12
-- ============================================================
