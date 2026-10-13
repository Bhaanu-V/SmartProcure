-- ==============================================================================
-- SmartProcure Schema Migration V4
-- User Authentication Fields and Master Data Seed
-- ==============================================================================

-- 1. Add authentication password and department_id foreign key to users
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'users' AND column_name = 'password') THEN
        ALTER TABLE users ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '$2a$10$wK1WwzT8VvO2yT8oN3q7Xe5Q2rK0dJ8uF6sB8hL9jM1oP4rS5tU6v';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'users' AND column_name = 'department_id') THEN
        ALTER TABLE users ADD COLUMN department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);

-- 2. Seed Core Departments if not existing
INSERT INTO departments (code, name, description)
VALUES
    ('ENG', 'Engineering', 'Software development, infrastructure, and DevOps'),
    ('FIN', 'Finance', 'Financial planning, accounting, and budgeting'),
    ('PROC', 'Procurement', 'Vendor management and strategic sourcing'),
    ('OPS', 'Operations', 'General business and administrative operations')
ON CONFLICT (code) DO NOTHING;

-- 3. Seed Initial Users if not existing (Default password is 'Password123!' hashed with BCrypt)
-- Hash: $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a
INSERT INTO users (username, email, password, full_name, role, department, department_id, is_active)
SELECT 'admin', 'admin@smartprocure.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'System Administrator', 'ADMIN', 'ENG', id, true
FROM departments WHERE code = 'ENG'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, full_name, role, department, department_id, is_active)
SELECT 'manager', 'manager@smartprocure.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Engineering Manager', 'MANAGER', 'ENG', id, true
FROM departments WHERE code = 'ENG'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, full_name, role, department, department_id, is_active)
SELECT 'finance', 'finance@smartprocure.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Finance Officer', 'FINANCE', 'FIN', id, true
FROM departments WHERE code = 'FIN'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, full_name, role, department, department_id, is_active)
SELECT 'procurement', 'procurement@smartprocure.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Procurement Specialist', 'PROCUREMENT', 'PROC', id, true
FROM departments WHERE code = 'PROC'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, full_name, role, department, department_id, is_active)
SELECT 'employee', 'employee@smartprocure.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Staff Engineer', 'EMPLOYEE', 'ENG', id, true
FROM departments WHERE code = 'ENG'
ON CONFLICT (username) DO NOTHING;

-- 4. Link Department Managers
UPDATE departments d
SET manager_id = u.id
FROM users u
WHERE d.code = 'ENG' AND u.username = 'manager' AND d.manager_id IS NULL;
