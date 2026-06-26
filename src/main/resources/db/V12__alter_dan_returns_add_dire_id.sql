-- Ensure dire_id, delivery_id, nd_type columns exist in dan_returns
-- (these may already be present if the table was created with them)
ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS dire_id     BIGINT;
ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS delivery_id BIGINT;
ALTER TABLE dan_returns ADD COLUMN IF NOT EXISTS nd_type     VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_dan_returns_dire_id    ON dan_returns(dire_id);
CREATE INDEX IF NOT EXISTS idx_dan_returns_delivery   ON dan_returns(delivery_id);
