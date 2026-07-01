ALTER TABLE loads
    ADD COLUMN IF NOT EXISTS offer_deadline_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS confirmation_deadline_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP;

UPDATE loads
SET offer_deadline_at = COALESCE(offer_deadline_at, created_at + INTERVAL '48 hours')
WHERE status = 'POSTED';

CREATE INDEX IF NOT EXISTS idx_loads_status_offer_deadline
    ON loads(status, offer_deadline_at);

CREATE INDEX IF NOT EXISTS idx_loads_status_confirmation_deadline
    ON loads(status, confirmation_deadline_at);
