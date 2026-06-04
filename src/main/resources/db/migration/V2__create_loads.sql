CREATE TABLE loads (
    id UUID PRIMARY KEY,

    broker_company_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,

    pickup_city VARCHAR(255) NOT NULL,
    pickup_state VARCHAR(255) NOT NULL,
    delivery_city VARCHAR(255) NOT NULL,
    delivery_state VARCHAR(255) NOT NULL,

    equipment_type VARCHAR(50) NOT NULL,
    weight INTEGER,
    rate NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,

    notes VARCHAR(1000),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_loads_broker_company
        FOREIGN KEY (broker_company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_loads_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
);