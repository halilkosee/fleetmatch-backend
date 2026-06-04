ALTER TABLE users
    RENAME COLUMN role TO platform_role;

UPDATE users
SET platform_role = 'USER'
WHERE platform_role IN ('BROKER', 'CARRIER');

UPDATE users
SET platform_role = 'ADMIN'
WHERE platform_role = 'ADMIN';

ALTER TABLE users
    ALTER COLUMN platform_role SET NOT NULL;