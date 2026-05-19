CREATE TABLE IF NOT EXISTS warehouse_master (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    lat         DOUBLE PRECISION,
    lon         DOUBLE PRECISION,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Sample warehouses in Bhubaneswar
INSERT INTO warehouse_master (name, address, lat, lon) VALUES
  ('Main Warehouse',   'Mancheswar Industrial Estate, Bhubaneswar', 20.2827, 85.8679),
  ('North Hub',        'Patia, Bhubaneswar',                        20.3526, 85.8194),
  ('South Depot',      'Jagamara, Bhubaneswar',                     20.2349, 85.8173)
ON CONFLICT DO NOTHING;
