-- ════════════════════════════════════════════════════════════════════════════
--  schema.sql  — run once against the target database
--  All DDL lives here; no CREATE/ALTER TABLE in Java @PostConstruct methods.
-- ════════════════════════════════════════════════════════════════════════════

-- ── Sequence: globally unique return-transaction ID ──────────────────────
CREATE SEQUENCE IF NOT EXISTS dan_return_dir_seq START 1000 INCREMENT 1;

-- ── stage_sales_entery: rename sales_id → dire_id ────────────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'stage_sales_entery' AND column_name = 'sales_id'
    ) THEN
        ALTER TABLE stage_sales_entery RENAME COLUMN sales_id TO dire_id;
    END IF;
END $$;

-- ── dan_returns ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dan_returns (
    id          BIGSERIAL     PRIMARY KEY,
    dir_id      BIGINT        NOT NULL,
    dire_id     BIGINT,
    delivery_id BIGINT        NOT NULL,
    picklist_no VARCHAR(50)   NOT NULL,
    nd_type     VARCHAR(20),
    serial      VARCHAR(100),
    description VARCHAR(255),
    bill_qty    INTEGER       DEFAULT 0,
    bill_amt    NUMERIC(14,2) DEFAULT 0,
    return_qty  INTEGER       DEFAULT 0,
    return_amt  NUMERIC(14,2) DEFAULT 0,
    reason      VARCHAR(255),
    is_custom   BOOLEAN       DEFAULT false,
    created_at  TIMESTAMP     DEFAULT NOW()
);

ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS dir_id      BIGINT;
ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS dire_id     BIGINT;
ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS delivery_id BIGINT;
ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS nd_type     VARCHAR(20);

-- ── delivery_status ───────────────────────────────────────────────────────
ALTER TABLE delivery_status ADD COLUMN IF NOT EXISTS dire_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_delivery_status_picklist_no'
    ) THEN
        ALTER TABLE delivery_status
            ADD CONSTRAINT uq_delivery_status_picklist_no UNIQUE (picklist_no);
    END IF;
END $$;

-- ── delivery_assignments ──────────────────────────────────────────────────
ALTER TABLE delivery_assignments ADD COLUMN IF NOT EXISTS dire_id BIGINT;

-- ── Backfill dire_id from stage_sales_entery ─────────────────────────────
UPDATE delivery_status ds
SET    dire_id = sse.dire_id
FROM   stage_sales_entery sse
WHERE  TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(ds.picklist_no))
  AND  ds.dire_id IS NULL;

UPDATE delivery_assignments da
SET    dire_id = sse.dire_id
FROM   stage_sales_entery sse
WHERE  TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(da.picklist_no))
  AND  da.dire_id IS NULL;

UPDATE dan_returns dr
SET    dire_id = sse.dire_id
FROM   stage_sales_entery sse
WHERE  TRIM(LOWER(sse.picklist_no)) = TRIM(LOWER(dr.picklist_no))
  AND  dr.dire_id IS NULL;

-- ── Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_stage_sales_entery_dire_id     ON stage_sales_entery(dire_id);
CREATE INDEX IF NOT EXISTS idx_delivery_status_dire_id        ON delivery_status(dire_id);
CREATE INDEX IF NOT EXISTS idx_delivery_assignments_dire_id   ON delivery_assignments(dire_id);
CREATE INDEX IF NOT EXISTS idx_dan_returns_dire_id            ON dan_returns(dire_id);
CREATE INDEX IF NOT EXISTS idx_dan_returns_dir_id             ON dan_returns(dir_id);
CREATE INDEX IF NOT EXISTS idx_dan_returns_delivery_picklist  ON dan_returns(delivery_id, picklist_no);
