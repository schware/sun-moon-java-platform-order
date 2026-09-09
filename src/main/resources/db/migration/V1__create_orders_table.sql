-- Postgres + JSONB: the whole Order is one document keyed by a bigserial
-- id, with customer_id pulled out as an indexed column. Designed as a
-- document store before MongoDB turned out to need AVX this CPU lacks —
-- see the umbrella repo's ADR-0001.
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id TEXT NOT NULL,
    data JSONB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id);
