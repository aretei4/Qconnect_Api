-- Add picklist_nos to dayend_approval
-- Stores comma-separated picklist numbers submitted with the day-end
-- e.g. 'E587P22657,E587P22658,E587P22659'
ALTER TABLE dayend_approval
    ADD COLUMN IF NOT EXISTS picklist_nos TEXT;
