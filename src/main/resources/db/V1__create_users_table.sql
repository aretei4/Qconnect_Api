-- ============================================================
-- Users table — role-based access control
-- Database: PostgreSQL (distr_v01)
-- Run once manually on the DB before starting the application.
-- Default user accounts are seeded by DataInitializer.java at startup.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100),
    email       VARCHAR(100),
    role        VARCHAR(20)  NOT NULL DEFAULT 'STAFF'
                             CONSTRAINT chk_users_role CHECK (role IN ('ADMIN','MANAGER','STAFF')),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);
