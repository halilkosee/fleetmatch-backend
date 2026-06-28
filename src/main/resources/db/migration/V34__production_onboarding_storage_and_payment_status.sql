ALTER TABLE company_documents
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;

ALTER TABLE company_subscriptions
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(100),
    ADD COLUMN IF NOT EXISTS external_subscription_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS external_customer_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_company_subscriptions_payment_status
    ON company_subscriptions(payment_status);

CREATE INDEX IF NOT EXISTS idx_company_documents_storage_key
    ON company_documents(storage_key);
