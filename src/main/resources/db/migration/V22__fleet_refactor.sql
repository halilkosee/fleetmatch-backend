-- V22__complete_fleet_refactor.sql

-- Column
ALTER TABLE offers
    RENAME COLUMN carrier_user_id TO fleet_user_id;

-- Foreign Key
ALTER TABLE offers
    RENAME CONSTRAINT fk_offer_carrier TO fk_offer_fleet;