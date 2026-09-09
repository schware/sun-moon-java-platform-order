CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id TEXT NOT NULL,
    data JSONB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id);

-- Added when orders gained a life cycle. ADD COLUMN IF NOT EXISTS keeps
-- this file idempotent, which is what spring.sql.init.mode=always needs;
-- with Flyway this would have been a numbered migration instead.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'PLACED';

-- Terminals ask "what is PLACED", so that must not scan parsed JSON.
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
