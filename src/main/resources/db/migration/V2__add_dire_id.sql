-- ── V2: Rename sales_id → dire_id and propagate to transaction tables ─────

-- 1. Rename sales_id to dire_id in stage_sales_entery
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'stage_sales_entery' AND column_name = 'sales_id'
    ) THEN
        ALTER TABLE stage_sales_entery RENAME COLUMN sales_id TO dire_id;
    END IF;
END $$;

-- 2. Add dire_id to downstream transaction tables
ALTER TABLE delivery_status      ADD COLUMN IF NOT EXISTS dire_id BIGINT;
ALTER TABLE delivery_assignments  ADD COLUMN IF NOT EXISTS dire_id BIGINT;
ALTER TABLE dan_returns           ADD COLUMN IF NOT EXISTS dire_id BIGINT;

-- 3. Backfill from stage_sales_entery.dire_id
UPDATE delivery_status ds
SET    dire_id = sse.dire_id
FROM   stage_sales_entery sse
WHERE  TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(ds.picklist_no));

UPDATE delivery_assignments da
SET    dire_id = sse.dire_id
FROM   stage_sales_entery sse
WHERE  TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(da.picklist_no));

UPDATE dan_returns dr
SET    dire_id = sse.dire_id
FROM   stage_sales_entery sse
WHERE  TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(dr.picklist_no));

-- 4. Indexes
CREATE INDEX IF NOT EXISTS idx_delivery_status_dire_id       ON delivery_status(dire_id);
CREATE INDEX IF NOT EXISTS idx_delivery_assignments_dire_id  ON delivery_assignments(dire_id);
CREATE INDEX IF NOT EXISTS idx_dan_returns_dire_id           ON dan_returns(dire_id);
