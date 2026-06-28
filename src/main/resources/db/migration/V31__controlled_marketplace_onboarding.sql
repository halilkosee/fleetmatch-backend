ALTER TABLE companies
    ADD COLUMN company_information_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN market_survey_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN manual_priority INTEGER,
    ADD COLUMN admin_internal_notes VARCHAR(1000);

UPDATE users
SET status = 'REGISTERED'
WHERE status = 'PENDING_VERIFICATION';

CREATE TABLE market_surveys (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL UNIQUE,
    company_type VARCHAR(50) NOT NULL,
    operating_states JSONB,
    equipment_types JSONB,
    average_loads_per_week INTEGER,
    fleet_size INTEGER,
    current_load_board VARCHAR(255),
    current_tms VARCHAR(255),
    future_integration_interest BOOLEAN,
    biggest_operational_challenges VARCHAR(4000),
    home_state VARCHAR(255),
    preferred_regions JSONB,
    preferred_mileage INTEGER,
    dedicated_route_interest BOOLEAN,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_market_surveys_company
        FOREIGN KEY (company_id)
            REFERENCES companies(id)
);

CREATE INDEX idx_market_surveys_company_type
    ON market_surveys(company_type);

CREATE INDEX idx_market_surveys_operating_states
    ON market_surveys USING GIN (operating_states);

CREATE INDEX idx_market_surveys_equipment_types
    ON market_surveys USING GIN (equipment_types);
