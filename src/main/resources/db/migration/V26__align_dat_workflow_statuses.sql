UPDATE offers
SET status = 'CONFIRMED'
WHERE status = 'ACCEPTED';

ALTER TABLE conversations
    ADD COLUMN archived_at TIMESTAMP;
