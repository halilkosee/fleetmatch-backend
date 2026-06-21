ALTER TABLE subscription_plans
    ADD COLUMN max_loads_per_month INTEGER;

ALTER TABLE company_subscriptions
    ADD COLUMN monthly_load_limit_override INTEGER;
