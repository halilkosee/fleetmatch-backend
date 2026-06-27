ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP;

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000);

ALTER TABLE loads
    ADD COLUMN IF NOT EXISTS weight_lbs INTEGER,
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS pickup_street_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pickup_zip_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pickup_location_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pickup_contact_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pickup_contact_phone VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pickup_time_window_start TIME,
    ADD COLUMN IF NOT EXISTS pickup_time_window_end TIME,
    ADD COLUMN IF NOT EXISTS pickup_instructions VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS delivery_street_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_zip_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_location_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_contact_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_contact_phone VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_time_window_start TIME,
    ADD COLUMN IF NOT EXISTS delivery_time_window_end TIME,
    ADD COLUMN IF NOT EXISTS delivery_instructions VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS pallet_count INTEGER,
    ADD COLUMN IF NOT EXISTS piece_count INTEGER,
    ADD COLUMN IF NOT EXISTS length_inches INTEGER,
    ADD COLUMN IF NOT EXISTS width_inches INTEGER,
    ADD COLUMN IF NOT EXISTS height_inches INTEGER,
    ADD COLUMN IF NOT EXISTS liftgate_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pallet_jack_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS dock_high_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS residential_delivery BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE loads
SET weight_lbs = weight
WHERE weight_lbs IS NULL;

CREATE TABLE IF NOT EXISTS user_verification_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    target_value VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_verification_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID,
    company_id UUID,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    read_at TIMESTAMP,
    related_entity_type VARCHAR(255),
    related_entity_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT fk_notifications_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    actor_email VARCHAR(255),
    actor_company_id UUID,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    details VARCHAR(2000),
    ip_address VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_companies_mc_number ON companies(mc_number);
CREATE INDEX IF NOT EXISTS idx_companies_dot_number ON companies(dot_number);
CREATE INDEX IF NOT EXISTS idx_companies_type ON companies(type);
CREATE INDEX IF NOT EXISTS idx_companies_verification_status ON companies(verification_status);

CREATE INDEX IF NOT EXISTS idx_loads_status ON loads(status);
CREATE INDEX IF NOT EXISTS idx_loads_pickup_state ON loads(pickup_state);
CREATE INDEX IF NOT EXISTS idx_loads_delivery_state ON loads(delivery_state);
CREATE INDEX IF NOT EXISTS idx_loads_pickup_date ON loads(pickup_date);
CREATE INDEX IF NOT EXISTS idx_loads_equipment_type ON loads(equipment_type);
CREATE INDEX IF NOT EXISTS idx_loads_broker_company ON loads(broker_company_id);

CREATE INDEX IF NOT EXISTS idx_offers_load ON offers(load_id);
CREATE INDEX IF NOT EXISTS idx_offers_fleet_user ON offers(fleet_user_id);
CREATE INDEX IF NOT EXISTS idx_offers_status ON offers(status);

CREATE INDEX IF NOT EXISTS idx_conversations_load ON conversations(load_id);
CREATE INDEX IF NOT EXISTS idx_conversations_archived_at ON conversations(archived_at);

CREATE INDEX IF NOT EXISTS idx_messages_sender_user ON messages(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_read_at ON messages(read_at);

CREATE INDEX IF NOT EXISTS idx_user_verification_codes_user_purpose
    ON user_verification_codes(user_id, purpose);
CREATE INDEX IF NOT EXISTS idx_user_verification_codes_expires_at
    ON user_verification_codes(expires_at);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_company ON notifications(company_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read_at ON notifications(read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_email ON audit_logs(actor_email);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
