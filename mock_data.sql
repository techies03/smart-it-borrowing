-- ============================================================
-- Mock data for TrackIT
-- Date window: 2026-03-05 to 2026-04-05
-- Assumes the schema/tables already exist
-- ============================================================

USE trackit;

START TRANSACTION;

-- Reuse the same BCrypt hash from schema.sql.
-- Password for all users below: admin123
SET @default_password_hash = '$2a$12$PnRn.8eDoIk76cSJLtdbs.ORy9CWKG1jILmSodPbAojTTCwbC.4fK';

-- ------------------------------------------------------------
-- Ensure required base records exist
-- ------------------------------------------------------------
INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'John Doe', 'john@trackit.com', @default_password_hash, 'USER', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'john@trackit.com'
);

INSERT INTO items (name, category, quantity, status, item_condition, image_url, created_at, updated_at)
SELECT 'Dell Laptop XPS 15', 'Laptop', 5, 'AVAILABLE', 'GOOD',
       '/static/uploads/items/28eefe53-5478-4760-9f3a-48be995e09fb.png',
       '2026-03-05 08:00:00', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM items WHERE name = 'Dell Laptop XPS 15'
);

INSERT INTO items (name, category, quantity, status, item_condition, image_url, created_at, updated_at)
SELECT 'HP LaserJet Printer', 'Printer', 2, 'AVAILABLE', 'GOOD',
       '/static/uploads/items/f46ecc0f-a7d7-4b8b-93e6-a19cda4ec1b0.png',
       '2026-03-05 08:00:00', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM items WHERE name = 'HP LaserJet Printer'
);

INSERT INTO items (name, category, quantity, status, item_condition, image_url, created_at, updated_at)
SELECT 'Logitech Webcam C920', 'Webcam', 8, 'AVAILABLE', 'GOOD',
       '/static/uploads/items/3e29e9bf-74e7-48fd-b53d-052f674e53e6.png',
       '2026-03-05 08:00:00', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM items WHERE name = 'Logitech Webcam C920'
);

INSERT INTO items (name, category, quantity, status, item_condition, image_url, created_at, updated_at)
SELECT 'HDMI to VGA Adapter', 'Accessory', 10, 'AVAILABLE', 'GOOD',
       '/static/uploads/items/38d36987-3e20-4a2e-b62d-d78ab391c459.png',
       '2026-03-05 08:00:00', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM items WHERE name = 'HDMI to VGA Adapter'
);

INSERT INTO items (name, category, quantity, status, item_condition, image_url, created_at, updated_at)
SELECT 'Wireless Mouse', 'Accessory', 15, 'AVAILABLE', 'GOOD',
       '/static/uploads/items/d54f2c49-c9de-4884-8fb0-b1294e4b6e0b.png',
       '2026-03-05 08:00:00', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM items WHERE name = 'Wireless Mouse'
);

INSERT INTO items (name, category, quantity, status, item_condition, image_url, created_at, updated_at)
SELECT 'TP-Link Network Switch', 'Networking', 3, 'AVAILABLE', 'GOOD',
       '/static/uploads/items/1ec8f94d-f53f-4232-a0b8-684fbd7c865c.png',
       '2026-03-05 08:00:00', '2026-03-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM items WHERE name = 'TP-Link Network Switch'
);

-- ------------------------------------------------------------
-- Additional Malay users
-- ------------------------------------------------------------
INSERT INTO users (name, email, password_hash, role, created_at)
VALUES
    ('Nur Aisyah binti Hakim', 'aisyah.hakim@trackit.com', @default_password_hash, 'USER', '2026-03-05 08:15:00'),
    ('Muhammad Firdaus bin Rizal', 'firdaus.rizal@trackit.com', @default_password_hash, 'USER', '2026-03-05 09:00:00'),
    ('Siti Nur Amirah binti Salleh', 'siti.amirah@trackit.com', @default_password_hash, 'USER', '2026-03-06 10:20:00'),
    ('Hafiz Rahman bin Musa', 'hafiz.rahman@trackit.com', @default_password_hash, 'USER', '2026-03-06 11:00:00'),
    ('Nurul Huda binti Karim', 'nurul.huda@trackit.com', @default_password_hash, 'USER', '2026-03-07 09:30:00'),
    ('Amirul Hakim bin Yusof', 'amirul.hakim@trackit.com', @default_password_hash, 'USER', '2026-03-07 12:10:00'),
    ('Puteri Balqis binti Omar', 'puteri.balqis@trackit.com', @default_password_hash, 'USER', '2026-03-08 08:45:00'),
    ('Farah Aisyah binti Zulkifli', 'farah.aisyah@trackit.com', @default_password_hash, 'USER', '2026-03-08 10:15:00'),
    ('Muhammad Izzat Danish bin Azlan', 'izzat.danish@trackit.com', @default_password_hash, 'USER', '2026-03-09 09:40:00'),
    ('Noraini binti Yusof', 'noraini.yusof@trackit.com', @default_password_hash, 'USER', '2026-03-09 11:25:00')
AS new
ON DUPLICATE KEY UPDATE
    name = new.name,
    password_hash = new.password_hash,
    role = new.role,
    created_at = new.created_at;

-- ------------------------------------------------------------
-- Bookings covering the last 1 month up to 2026-04-05
-- Mix of RETURNED, APPROVED, RETURN_PENDING, PENDING, REJECTED
-- ------------------------------------------------------------
INSERT INTO bookings (
    user_id,
    item_id,
    quantity,
    borrow_date,
    return_date,
    actual_return_date,
    status,
    penalty,
    condition_before,
    condition_after,
    created_at,
    updated_at
)
VALUES
    ((SELECT id FROM users WHERE email = 'john@trackit.com' LIMIT 1),           (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-03-06 09:00:00', '2026-03-08 17:00:00', '2026-03-08 15:20:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-05 15:10:00', '2026-03-08 15:20:00'),
    ((SELECT id FROM users WHERE email = 'aisyah.hakim@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-03-07 10:00:00', '2026-03-09 17:00:00', '2026-03-10 09:15:00', 'RETURNED',       5.00, 'GOOD', 'GOOD',    '2026-03-06 11:25:00', '2026-03-10 09:15:00'),
    ((SELECT id FROM users WHERE email = 'firdaus.rizal@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'HP LaserJet Printer' LIMIT 1),     1, '2026-03-08 08:30:00', '2026-03-10 12:00:00', NULL,                  'REJECTED',       0.00, 'GOOD', NULL,      '2026-03-07 09:10:00', '2026-03-07 16:30:00'),
    ((SELECT id FROM users WHERE email = 'siti.amirah@trackit.com' LIMIT 1),    (SELECT id FROM items WHERE name = 'Wireless Mouse' LIMIT 1),          1, '2026-03-09 14:00:00', '2026-03-11 18:00:00', '2026-03-11 17:45:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-08 10:00:00', '2026-03-11 17:45:00'),
    ((SELECT id FROM users WHERE email = 'hafiz.rahman@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'TP-Link Network Switch' LIMIT 1),  1, '2026-03-10 09:30:00', '2026-03-12 17:30:00', '2026-03-14 10:00:00', 'RETURNED',      10.00, 'GOOD', 'GOOD',    '2026-03-09 13:40:00', '2026-03-14 10:00:00'),
    ((SELECT id FROM users WHERE email = 'nurul.huda@trackit.com' LIMIT 1),     (SELECT id FROM items WHERE name = 'HDMI to VGA Adapter' LIMIT 1),     1, '2026-03-12 08:00:00', '2026-03-12 17:00:00', '2026-03-12 16:00:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-11 09:00:00', '2026-03-12 16:00:00'),
    ((SELECT id FROM users WHERE email = 'amirul.hakim@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-03-14 09:00:00', '2026-03-16 17:00:00', '2026-03-16 15:30:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-13 16:20:00', '2026-03-16 15:30:00'),
    ((SELECT id FROM users WHERE email = 'puteri.balqis@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-03-15 10:00:00', '2026-03-17 17:00:00', '2026-03-17 18:10:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-14 09:10:00', '2026-03-17 18:10:00'),
    ((SELECT id FROM users WHERE email = 'farah.aisyah@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'HP LaserJet Printer' LIMIT 1),     1, '2026-03-16 09:00:00', '2026-03-18 17:00:00', '2026-03-18 16:30:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-15 13:00:00', '2026-03-18 16:30:00'),
    ((SELECT id FROM users WHERE email = 'izzat.danish@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Wireless Mouse' LIMIT 1),          1, '2026-03-17 11:00:00', '2026-03-19 18:00:00', '2026-03-20 09:00:00', 'RETURNED',       5.00, 'GOOD', 'GOOD',    '2026-03-16 08:30:00', '2026-03-20 09:00:00'),
    ((SELECT id FROM users WHERE email = 'noraini.yusof@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-03-18 13:30:00', '2026-03-20 17:00:00', '2026-03-20 14:00:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-17 10:20:00', '2026-03-20 14:00:00'),
    ((SELECT id FROM users WHERE email = 'john@trackit.com' LIMIT 1),           (SELECT id FROM items WHERE name = 'TP-Link Network Switch' LIMIT 1),  1, '2026-03-19 09:00:00', '2026-03-21 17:00:00', '2026-03-21 12:10:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-18 14:30:00', '2026-03-21 12:10:00'),
    ((SELECT id FROM users WHERE email = 'aisyah.hakim@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Wireless Mouse' LIMIT 1),          1, '2026-03-20 08:30:00', '2026-03-22 17:00:00', '2026-03-25 09:30:00', 'RETURNED',      15.00, 'GOOD', 'GOOD',    '2026-03-19 16:00:00', '2026-03-25 09:30:00'),
    ((SELECT id FROM users WHERE email = 'firdaus.rizal@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-03-21 10:00:00', '2026-03-23 17:00:00', '2026-03-23 16:20:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-20 11:10:00', '2026-03-23 16:20:00'),
    ((SELECT id FROM users WHERE email = 'siti.amirah@trackit.com' LIMIT 1),    (SELECT id FROM items WHERE name = 'HP LaserJet Printer' LIMIT 1),     1, '2026-03-22 09:30:00', '2026-03-23 17:00:00', NULL,                  'REJECTED',       0.00, 'GOOD', NULL,      '2026-03-21 12:10:00', '2026-03-21 16:40:00'),
    ((SELECT id FROM users WHERE email = 'hafiz.rahman@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-03-24 09:00:00', '2026-03-26 17:00:00', '2026-03-26 16:45:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-23 13:00:00', '2026-03-26 16:45:00'),
    ((SELECT id FROM users WHERE email = 'nurul.huda@trackit.com' LIMIT 1),     (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-03-25 14:00:00', '2026-03-27 17:00:00', '2026-03-27 14:50:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-24 10:40:00', '2026-03-27 14:50:00'),
    ((SELECT id FROM users WHERE email = 'amirul.hakim@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'HDMI to VGA Adapter' LIMIT 1),     1, '2026-03-27 08:30:00', '2026-03-27 18:00:00', NULL,                  'REJECTED',       0.00, 'GOOD', NULL,      '2026-03-26 09:15:00', '2026-03-26 14:20:00'),
    ((SELECT id FROM users WHERE email = 'puteri.balqis@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'Wireless Mouse' LIMIT 1),          1, '2026-03-28 09:00:00', '2026-03-30 17:00:00', '2026-03-30 16:00:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-27 08:50:00', '2026-03-30 16:00:00'),
    ((SELECT id FROM users WHERE email = 'farah.aisyah@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-03-29 09:00:00', '2026-03-31 17:00:00', '2026-04-01 08:10:00', 'RETURNED',       5.00, 'GOOD', 'GOOD',    '2026-03-28 10:35:00', '2026-04-01 08:10:00'),
    ((SELECT id FROM users WHERE email = 'izzat.danish@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-03-30 10:30:00', '2026-04-01 17:00:00', '2026-04-01 16:20:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-29 15:10:00', '2026-04-01 16:20:00'),
    ((SELECT id FROM users WHERE email = 'noraini.yusof@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'HDMI to VGA Adapter' LIMIT 1),     1, '2026-03-31 09:00:00', '2026-04-01 17:30:00', '2026-04-01 17:00:00', 'RETURNED',       0.00, 'GOOD', 'GOOD',    '2026-03-30 09:45:00', '2026-04-01 17:00:00'),
    ((SELECT id FROM users WHERE email = 'john@trackit.com' LIMIT 1),           (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-04-01 09:00:00', '2026-04-03 17:00:00', NULL,                  'APPROVED',       0.00, 'GOOD', NULL,      '2026-03-31 11:15:00', '2026-04-01 12:00:00'),
    ((SELECT id FROM users WHERE email = 'aisyah.hakim@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'HP LaserJet Printer' LIMIT 1),     1, '2026-04-02 09:00:00', '2026-04-04 17:00:00', '2026-04-05 10:00:00', 'RETURN_PENDING', 5.00, 'GOOD', 'GOOD',    '2026-04-01 13:00:00', '2026-04-05 10:00:00'),
    ((SELECT id FROM users WHERE email = 'firdaus.rizal@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'TP-Link Network Switch' LIMIT 1),  1, '2026-04-03 08:30:00', '2026-04-05 16:30:00', NULL,                  'APPROVED',       0.00, 'GOOD', NULL,      '2026-04-02 09:10:00', '2026-04-03 12:00:00'),
    ((SELECT id FROM users WHERE email = 'siti.amirah@trackit.com' LIMIT 1),    (SELECT id FROM items WHERE name = 'Wireless Mouse' LIMIT 1),          1, '2026-04-03 11:00:00', '2026-04-05 18:00:00', NULL,                  'PENDING',        0.00, 'GOOD', NULL,      '2026-04-02 11:20:00', '2026-04-02 11:20:00'),
    ((SELECT id FROM users WHERE email = 'hafiz.rahman@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-04-04 09:30:00', '2026-04-05 17:00:00', '2026-04-05 09:15:00', 'RETURN_PENDING', 0.00, 'GOOD', 'DAMAGED', '2026-04-03 10:05:00', '2026-04-05 09:15:00'),
    ((SELECT id FROM users WHERE email = 'nurul.huda@trackit.com' LIMIT 1),     (SELECT id FROM items WHERE name = 'HDMI to VGA Adapter' LIMIT 1),     1, '2026-04-04 08:00:00', '2026-04-04 18:00:00', NULL,                  'APPROVED',       0.00, 'GOOD', NULL,      '2026-04-03 09:30:00', '2026-04-04 07:45:00'),
    ((SELECT id FROM users WHERE email = 'amirul.hakim@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'HP LaserJet Printer' LIMIT 1),     1, '2026-04-04 10:00:00', '2026-04-05 12:00:00', NULL,                  'PENDING',        0.00, 'GOOD', NULL,      '2026-04-03 15:40:00', '2026-04-03 15:40:00'),
    ((SELECT id FROM users WHERE email = 'puteri.balqis@trackit.com' LIMIT 1),  (SELECT id FROM items WHERE name = 'Dell Laptop XPS 15' LIMIT 1),     1, '2026-04-05 09:00:00', '2026-04-05 17:00:00', NULL,                  'APPROVED',       0.00, 'GOOD', NULL,      '2026-04-04 14:10:00', '2026-04-05 08:00:00'),
    ((SELECT id FROM users WHERE email = 'farah.aisyah@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Wireless Mouse' LIMIT 1),          1, '2026-04-05 10:00:00', '2026-04-05 18:00:00', NULL,                  'PENDING',        0.00, 'GOOD', NULL,      '2026-04-04 16:25:00', '2026-04-04 16:25:00'),
    ((SELECT id FROM users WHERE email = 'izzat.danish@trackit.com' LIMIT 1),   (SELECT id FROM items WHERE name = 'Logitech Webcam C920' LIMIT 1),    1, '2026-04-05 13:00:00', '2026-04-05 18:00:00', NULL,                  'PENDING',        0.00, 'GOOD', NULL,      '2026-04-04 17:05:00', '2026-04-04 17:05:00');

COMMIT;

-- End of mock data file

