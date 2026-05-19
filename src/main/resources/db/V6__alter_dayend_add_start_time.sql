-- Add start_time to track when agent begins the day-end process
-- Status flow: STARTED → PENDING → APPROVED / REJECTED
ALTER TABLE dayend_approval
    ADD COLUMN IF NOT EXISTS start_time TIMESTAMP;
