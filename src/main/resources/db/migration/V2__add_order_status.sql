-- Orders gained a life cycle. status is a real column rather than a JSON
-- field because terminals ask "what is PLACED", and that must not scan
-- parsed JSON.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'PLACED';

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
