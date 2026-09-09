-- Rows written before the life cycle existed have no status, no
-- placedAt and no updatedAt inside their JSONB. They deserialize with
-- nulls, and the expiry sweep dereferenced one of them - so two rows from
-- an older schema stopped every order in the system from expiring.
--
-- They are retired rather than repaired: nobody is going to accept an
-- order placed against a schema that no longer exists, and inventing a
-- placedAt for them would only make them look like orders that are
-- merely late.
--
-- This migration is the reason this service moved off schema.sql. An
-- idempotent CREATE can add a column; it cannot fix the rows that were
-- already there.
UPDATE orders
SET status = 'EXPIRED',
    data = data || jsonb_build_object(
        'status', 'EXPIRED',
        'acceptedBy', NULL,
        'placedAt', to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
        'updatedAt', to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'))
WHERE data->>'placedAt' IS NULL;
