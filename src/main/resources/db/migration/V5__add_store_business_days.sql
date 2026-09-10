-- 영업일. A store is 개점 or 마감, and every order it takes is stamped with
-- the 영업일자 that was open at the time — which is not the same as the
-- calendar date the order arrived on, because a shop trading past midnight
-- is still on yesterday's business day.
--
-- This lives in Order rather than in BO (which owns 매장 기준 정보) because
-- it is read on the write path: every order stamps a 매출일자 and every
-- acceptance mints a 거래번호 against it. A cross-service call inside the
-- sale would be the wrong shape.
CREATE TABLE IF NOT EXISTS store_business_days (
    store_id VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    opened_by VARCHAR(64) NOT NULL,
    closed_at TIMESTAMPTZ,
    closed_by VARCHAR(64),
    PRIMARY KEY (store_id, business_date)
);

-- At most one open day per store, enforced by the database rather than by
-- a read-then-write in the service: two terminals pressing 개점 at once is
-- exactly the race that would otherwise slip through.
CREATE UNIQUE INDEX IF NOT EXISTS idx_store_business_days_one_open
    ON store_business_days (store_id) WHERE closed_at IS NULL;

-- A real column, not JSONB: 매출은 일 단위로 관리된다 means this is queried
-- on, and the same reasoning applied to store_id in V4.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS business_date DATE;

CREATE INDEX IF NOT EXISTS idx_orders_business_date ON orders (business_date, store_id);
