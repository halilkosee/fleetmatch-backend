ALTER TABLE companies
    ADD COLUMN entity_type VARCHAR(100),
    ADD COLUMN ein VARCHAR(50),
    ADD COLUMN state_of_formation VARCHAR(100),
    ADD COLUMN headquarters VARCHAR(255),
    ADD COLUMN primary_contact VARCHAR(255),
    ADD COLUMN authority_status VARCHAR(100),
    ADD COLUMN broker_bond_or_trust VARCHAR(100),
    ADD COLUMN insurance_coverage VARCHAR(100),
    ADD COLUMN operating_regions VARCHAR(1000);
