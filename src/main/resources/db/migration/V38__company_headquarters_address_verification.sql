ALTER TABLE companies
    ADD COLUMN normalized_headquarters VARCHAR(255),
    ADD COLUMN headquarters_address_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN headquarters_address_verification_status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN headquarters_latitude DOUBLE PRECISION,
    ADD COLUMN headquarters_longitude DOUBLE PRECISION;

UPDATE companies
SET headquarters_address_verification_status = 'PENDING'
WHERE headquarters IS NOT NULL
  AND headquarters_address_verification_status IS NULL;
