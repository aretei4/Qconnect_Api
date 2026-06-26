-- Change the unique lookup key on stage_sales_entery from picklist_no to sales_order_no.
-- Drop existing unique constraint / index on picklist_no if present (ignore if not exists).
DO $$
BEGIN
    -- drop unique constraint by name if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'stage_sales_entery_picklist_no_key'
          AND conrelid = 'stage_sales_entery'::regclass
    ) THEN
        ALTER TABLE stage_sales_entery DROP CONSTRAINT stage_sales_entery_picklist_no_key;
    END IF;

    -- drop unique index on picklist_no if it exists separately
    IF EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'stage_sales_entery'
          AND indexname  = 'idx_stage_sales_picklist_no_unique'
    ) THEN
        DROP INDEX idx_stage_sales_picklist_no_unique;
    END IF;
END $$;

-- Add unique constraint on sales_order_no
ALTER TABLE stage_sales_entery
    ADD CONSTRAINT uq_stage_sales_order_no UNIQUE (sales_order_no);
