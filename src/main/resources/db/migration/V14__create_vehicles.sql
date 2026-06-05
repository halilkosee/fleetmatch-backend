CREATE TABLE vehicles
(
    id UUID PRIMARY KEY,

    company_id UUID NOT NULL,

    type VARCHAR(50) NOT NULL,

    length_feet INTEGER NOT NULL,

    make VARCHAR(100),

    model VARCHAR(100),

    year INTEGER,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_vehicle_company
        FOREIGN KEY (company_id)
            REFERENCES companies(id)
);