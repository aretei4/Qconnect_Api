-- Make dire_id the primary conflict key; remove picklist_no
ALTER TABLE delivery_assignments ALTER COLUMN dire_id SET NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_delivery_assignments_dire_id'
    ) THEN
        ALTER TABLE delivery_assignments ADD CONSTRAINT uq_delivery_assignments_dire_id UNIQUE (dire_id);
    END IF;
END$$;
ALTER TABLE delivery_assignments DROP COLUMN IF EXISTS picklist_no;
