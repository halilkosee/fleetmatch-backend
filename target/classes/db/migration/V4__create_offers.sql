CREATE TABLE offers (
                        id UUID PRIMARY KEY,

                        load_id UUID NOT NULL,
                        carrier_user_id UUID NOT NULL,

                        amount NUMERIC(12,2) NOT NULL,
                        message VARCHAR(1000),

                        status VARCHAR(50) NOT NULL,

                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL,

                        CONSTRAINT fk_offer_load
                            FOREIGN KEY (load_id)
                                REFERENCES loads(id),

                        CONSTRAINT fk_offer_carrier
                            FOREIGN KEY (carrier_user_id)
                                REFERENCES users(id)
);