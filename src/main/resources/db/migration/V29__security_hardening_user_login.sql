ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS credentials_changed_at TIMESTAMP;

UPDATE users
SET credentials_changed_at = COALESCE(credentials_changed_at, created_at)
WHERE credentials_changed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);
