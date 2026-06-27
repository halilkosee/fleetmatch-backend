ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS verification_notes VARCHAR(2000);
