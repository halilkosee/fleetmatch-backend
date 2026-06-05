CREATE TABLE vehicle_capabilities
(
    vehicle_id UUID NOT NULL,

    capability VARCHAR(50) NOT NULL,

    CONSTRAINT fk_vehicle_capability_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES vehicles(id)
);