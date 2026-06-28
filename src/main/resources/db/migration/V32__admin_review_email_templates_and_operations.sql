ALTER TABLE companies
    ADD COLUMN rejection_reason VARCHAR(2000),
    ADD COLUMN additional_documents_request VARCHAR(2000);

CREATE TABLE email_templates (
    id UUID PRIMARY KEY,
    template_key VARCHAR(100) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    body VARCHAR(10000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO email_templates (
    id,
    template_key,
    subject,
    body,
    active,
    created_at,
    updated_at
) VALUES
(
    gen_random_uuid(),
    'welcome',
    'Welcome to EasyFleetMatch',
    'Welcome to EasyFleetMatch. Your account has been created. Please complete email verification, phone verification, company information, document upload, and the market survey to submit your company for review.',
    TRUE,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'verification_approved',
    'Your EasyFleetMatch company has been approved',
    'Congratulations! {{companyName}} has been approved. You may now choose a subscription plan and begin using the EasyFleetMatch marketplace.',
    TRUE,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'verification_rejected',
    'Your EasyFleetMatch verification needs attention',
    'Your company verification could not be completed. Reason: {{reason}}. Please update the requested information and submit your company for review again.',
    TRUE,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'additional_documents_requested',
    'Additional verification documents requested',
    'EasyFleetMatch operations requested additional documents for {{companyName}}. Details: {{reason}}. Please upload the requested documents and submit your company for review again.',
    TRUE,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'subscription_available',
    'Subscription plans are now available',
    'Your company has been approved. You can now choose a subscription plan and start using EasyFleetMatch.',
    TRUE,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'subscription_expiring',
    'Your EasyFleetMatch subscription is expiring soon',
    'Your subscription is expiring soon. Please review your subscription settings to avoid interruption.',
    TRUE,
    NOW(),
    NOW()
);
