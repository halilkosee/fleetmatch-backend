-- CARRIER -> FLEET refactor

UPDATE companies
SET type = 'FLEET'
WHERE type = 'CARRIER';

ALTER TABLE companies
DROP COLUMN carrier_type;