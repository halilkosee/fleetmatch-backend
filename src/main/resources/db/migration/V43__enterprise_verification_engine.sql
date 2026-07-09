CREATE TABLE company_verification_snapshots (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    submitted_by_user_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    company_type VARCHAR(50) NOT NULL,
    user_status VARCHAR(50) NOT NULL,
    company_verification_status VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    completion_percentage INTEGER NOT NULL,
    submission_ready BOOLEAN NOT NULL,
    completed_sections JSONB,
    incomplete_sections JSONB,
    missing_fields JSONB,
    missing_documents JSONB,
    invalid_fields JSONB,
    warnings JSONB,
    blocking_errors JSONB,
    legal_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    mc_number VARCHAR(100),
    dot_number VARCHAR(100),
    ein VARCHAR(50),
    headquarters VARCHAR(255),
    website VARCHAR(255),
    fleet_size INTEGER,
    active_vehicle_count BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_company_verification_snapshots_company
        FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_company_verification_snapshots_submitted_by
        FOREIGN KEY (submitted_by_user_id) REFERENCES users(id),
    CONSTRAINT ux_company_verification_snapshots_company_version
        UNIQUE (company_id, version_number)
);

CREATE INDEX idx_company_verification_snapshots_company_created
    ON company_verification_snapshots(company_id, created_at DESC);

CREATE TABLE company_verification_risk_assessments (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    snapshot_id UUID,
    score INTEGER NOT NULL,
    level VARCHAR(50) NOT NULL,
    signals JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_company_verification_risk_company
        FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_company_verification_risk_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES company_verification_snapshots(id)
);

CREATE INDEX idx_company_verification_risk_company_created
    ON company_verification_risk_assessments(company_id, created_at DESC);

CREATE INDEX idx_company_verification_risk_level
    ON company_verification_risk_assessments(level);

CREATE TABLE company_verification_checklist_items (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    snapshot_id UUID,
    item_key VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    mandatory BOOLEAN NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(1000),
    reviewer_user_id UUID,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_company_verification_checklist_company
        FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_company_verification_checklist_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES company_verification_snapshots(id),
    CONSTRAINT fk_company_verification_checklist_reviewer
        FOREIGN KEY (reviewer_user_id) REFERENCES users(id),
    CONSTRAINT ux_company_verification_checklist_snapshot_item
        UNIQUE (snapshot_id, item_key)
);

CREATE INDEX idx_company_verification_checklist_snapshot
    ON company_verification_checklist_items(snapshot_id);

CREATE TABLE company_verification_section_reviews (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    snapshot_id UUID,
    section_key VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes VARCHAR(1000),
    reviewer_user_id UUID,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_company_verification_sections_company
        FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_company_verification_sections_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES company_verification_snapshots(id),
    CONSTRAINT fk_company_verification_sections_reviewer
        FOREIGN KEY (reviewer_user_id) REFERENCES users(id),
    CONSTRAINT ux_company_verification_sections_snapshot_section
        UNIQUE (snapshot_id, section_key)
);

CREATE INDEX idx_company_verification_sections_snapshot
    ON company_verification_section_reviews(snapshot_id);
