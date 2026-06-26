-- DAN Close: return items recorded during the day-end close flow.
-- dan_id references dayend_approval.id (the DAN is the dayend_approval record).
CREATE TABLE IF NOT EXISTS dan_returns (
    id           BIGSERIAL    PRIMARY KEY,
    dan_id       BIGINT       NOT NULL REFERENCES dayend_approval(id) ON DELETE CASCADE,
    picklist_no  VARCHAR(100) NOT NULL,
    serial       VARCHAR(100),
    description  TEXT,
    bill_qty     INT          NOT NULL DEFAULT 0,
    bill_amt     NUMERIC(12,2) NOT NULL DEFAULT 0,
    return_qty   INT          NOT NULL DEFAULT 0,
    return_amt   NUMERIC(12,2) NOT NULL DEFAULT 0,
    reason       TEXT,
    is_custom    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dan_returns_dan_id ON dan_returns(dan_id);
CREATE INDEX IF NOT EXISTS idx_dan_returns_picklist ON dan_returns(picklist_no);
