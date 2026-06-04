ALTER TABLE users
    RENAME COLUMN carrier_user_type TO company_user_role;

UPDATE users
SET company_user_role = 'OWNER'
WHERE company_user_role IS NULL;

ALTER TABLE users
    ALTER COLUMN company_user_role SET NOT NULL;