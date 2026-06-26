-- ── Feature configuration table ───────────────────────────────────────────────
-- Each row is one feature flag.
-- feature_key : unique identifier used by the backend (e.g. 'OTP_VERIFICATION')
-- enabled     : true = feature is on, false = off
-- label       : human-readable name shown in the UI
-- description : one-line explanation shown below the toggle
-- category    : groups features on the settings page
-- updated_at  : last time the flag was changed

CREATE TABLE IF NOT EXISTS feature_config (
    feature_key VARCHAR(100) PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    label       VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(100) NOT NULL DEFAULT 'General',
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Default feature flags ──────────────────────────────────────────────────────

INSERT INTO feature_config (feature_key, enabled, label, description, category) VALUES

  -- Delivery
  ('OTP_VERIFICATION',         TRUE,  'OTP Verification',           'Require OTP from customer to confirm delivery',                 'Delivery'),
  ('SMART_ROUTE',              TRUE,  'Smart Route',                'Enable smart route optimisation for delivery agents',           'Delivery'),
  ('DELIVERY_MAP',             TRUE,  'Delivery Map',               'Show real-time delivery locations on map',                      'Delivery'),
  ('CUSTOMER_LOCATION_UPDATE', TRUE,  'Customer Location Update',   'Auto-update customer coordinates from delivery GPS',            'Delivery'),

  -- Day End
  ('DAY_END_APPROVAL',         TRUE,  'Day-End Approval',           'Require manager approval before day-end is closed',             'Day End'),
  ('DAY_END_AUTO_REJECT',      FALSE, 'Auto-Reject After 24h',      'Automatically reject pending day-end after 24 hours',           'Day End'),

  -- Payment Modes
  ('PAYMENT_CASH',             TRUE,  'Cash Payment',               'Allow CASH as payment mode in delivery app',                    'Payment Modes'),
  ('PAYMENT_UPI',              TRUE,  'UPI Payment',                'Allow UPI as payment mode in delivery app',                     'Payment Modes'),
  ('PAYMENT_CHEQUE',           TRUE,  'Cheque Payment',             'Allow CHEQUE as payment mode in delivery app',                  'Payment Modes'),
  ('PAYMENT_NEFT',             TRUE,  'NEFT / Bank Transfer',       'Allow NEFT as payment mode in delivery app',                    'Payment Modes'),
  ('PAYMENT_CARD',             FALSE, 'Card Payment',               'Allow CARD as payment mode in delivery app',                    'Payment Modes'),

  -- Operations
  ('PARTIAL_DELIVERY',         FALSE, 'Partial Delivery',           'Allow agents to mark partial delivery for a picklist',          'Operations'),
  ('ROUTE_SHARING',            FALSE, 'Route Sharing',              'Allow agents to share their route link with customers',         'Operations'),
  ('SALES_EDIT',               TRUE,  'Sales Edit',                 'Allow staff to edit sales entry values in the portal',          'Operations'),

  -- Dashboard
  ('DASHBOARD_NET_VALUE',      TRUE,  'Show Net Value on Dashboard','Display net value totals on the delivery dashboard',            'Dashboard')

ON CONFLICT (feature_key) DO NOTHING;
