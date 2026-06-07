CREATE TABLE subscription_plans (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP NOT NULL,

                                    name VARCHAR(255) NOT NULL UNIQUE,
                                    description VARCHAR(1000),

                                    monthly_price NUMERIC(10,2) NOT NULL,

                                    max_vehicles INTEGER,
                                    max_users INTEGER,
                                    max_loads_visible INTEGER,

                                    can_submit_offers BOOLEAN NOT NULL,
                                    can_view_contact_info BOOLEAN NOT NULL,

                                    active BOOLEAN NOT NULL
);

CREATE TABLE company_subscriptions (
                                       id UUID PRIMARY KEY,
                                       created_at TIMESTAMP NOT NULL,
                                       updated_at TIMESTAMP NOT NULL,

                                       company_id UUID NOT NULL,
                                       subscription_plan_id UUID NOT NULL,

                                       start_date DATE,
                                       end_date DATE,

                                       active BOOLEAN NOT NULL,
                                       auto_renew BOOLEAN NOT NULL,

                                       custom_price NUMERIC(10,2),

                                       vehicle_limit_override INTEGER,
                                       user_limit_override INTEGER,
                                       load_limit_override INTEGER,

                                       can_submit_offers_override BOOLEAN,
                                       can_view_contact_info_override BOOLEAN,

                                       CONSTRAINT fk_company_subscription_company
                                           FOREIGN KEY (company_id)
                                               REFERENCES companies(id),

                                       CONSTRAINT fk_company_subscription_plan
                                           FOREIGN KEY (subscription_plan_id)
                                               REFERENCES subscription_plans(id)
);