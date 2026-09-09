-- Orders belong to a store. Until now there was one implicit shop, which
-- worked only because there was one — with two, "is a POS connected" would
-- have been answered by whichever branch happened to have one, and an
-- order would have gone to a counter that never placed it.
--
-- A column rather than only a JSON field: routing and every operator
-- question ("what is waiting at this shop") filters on it.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS store_id TEXT;

-- Rows from before stores existed have no shop to attribute them to, and
-- guessing one would put real-looking orders in a real-looking branch.
-- They are marked as belonging to none.
UPDATE orders SET store_id = 'unknown' WHERE store_id IS NULL;
UPDATE orders
SET data = data || jsonb_build_object('storeId', COALESCE(data->>'storeId', 'unknown'))
WHERE data->>'storeId' IS NULL;

ALTER TABLE orders ALTER COLUMN store_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_store_status ON orders (store_id, status);
