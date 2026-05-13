-- ============================================================
-- Add extended agent fields to delivery_master
-- Run once on the database before deploying the updated backend.
-- ============================================================

ALTER TABLE delivery_master
    ADD COLUMN IF NOT EXISTS alt_mobile      VARCHAR(15),
    ADD COLUMN IF NOT EXISTS address1        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address2        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address3        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pin_code        VARCHAR(10),
    ADD COLUMN IF NOT EXISTS father_name     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS aadhar_no       VARCHAR(12),
    ADD COLUMN IF NOT EXISTS pan_card        VARCHAR(10),
    ADD COLUMN IF NOT EXISTS bank_account    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS date_of_joining DATE;
