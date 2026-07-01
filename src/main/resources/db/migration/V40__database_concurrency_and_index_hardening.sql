ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE company_documents
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE loads
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE offers
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE company_subscriptions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_loads_status_pickup_date
    ON loads(status, pickup_date);

CREATE INDEX IF NOT EXISTS idx_loads_status_equipment_pickup
    ON loads(status, equipment_type, pickup_date);

CREATE INDEX IF NOT EXISTS idx_loads_broker_status_created
    ON loads(broker_company_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_offers_load_status
    ON offers(load_id, status);

CREATE INDEX IF NOT EXISTS idx_offers_fleet_status
    ON offers(fleet_user_id, status);

CREATE INDEX IF NOT EXISTS idx_offers_created_at
    ON offers(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_vehicles_company_active
    ON vehicles(company_id, active);

CREATE INDEX IF NOT EXISTS idx_vehicles_company_type_active
    ON vehicles(company_id, type, active);

CREATE INDEX IF NOT EXISTS idx_vehicles_status
    ON vehicles(status);

CREATE INDEX IF NOT EXISTS idx_company_subscriptions_company_active_payment
    ON company_subscriptions(company_id, active, payment_status);

CREATE INDEX IF NOT EXISTS idx_company_subscriptions_end_date
    ON company_subscriptions(end_date);

CREATE INDEX IF NOT EXISTS idx_company_documents_company_review_status
    ON company_documents(company_id, review_status);

CREATE INDEX IF NOT EXISTS idx_company_documents_company_document_type
    ON company_documents(company_id, document_type);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread_created
    ON notifications(user_id, read_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_company_unread_created
    ON notifications(company_id, read_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_company
    ON audit_logs(actor_company_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_created
    ON audit_logs(entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_company_status_created
    ON support_tickets(company_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_priority_created
    ON support_tickets(priority, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_user_verification_codes_user_purpose_expires
    ON user_verification_codes(user_id, purpose, expires_at);

CREATE UNIQUE INDEX IF NOT EXISTS ux_company_subscriptions_one_active
    ON company_subscriptions(company_id)
    WHERE active = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_offers_one_selected_or_confirmed_per_load
    ON offers(load_id)
    WHERE status IN ('SELECTED', 'CONFIRMED');

CREATE UNIQUE INDEX IF NOT EXISTS ux_vehicle_capabilities_vehicle_capability
    ON vehicle_capabilities(vehicle_id, capability);
