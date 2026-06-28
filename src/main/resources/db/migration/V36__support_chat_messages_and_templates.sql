CREATE TABLE support_ticket_messages (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ticket_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    CONSTRAINT fk_support_message_ticket
        FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    CONSTRAINT fk_support_message_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(id)
);

CREATE INDEX idx_support_messages_ticket_created
    ON support_ticket_messages(ticket_id, created_at ASC);

CREATE TABLE support_reply_templates (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    template_key VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_support_reply_templates_category
    ON support_reply_templates(category);

ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resolution_summary VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS satisfaction_rating INTEGER,
    ADD COLUMN IF NOT EXISTS satisfaction_comment VARCHAR(1000);

INSERT INTO support_reply_templates (
    id,
    created_at,
    updated_at,
    template_key,
    title,
    category,
    body,
    active
)
VALUES
    (
        gen_random_uuid(),
        NOW(),
        NOW(),
        'documents_received',
        'Documents received',
        'DOCUMENTS',
        'Thanks for uploading the documents. Our operations team will review them and update your onboarding status shortly.',
        TRUE
    ),
    (
        gen_random_uuid(),
        NOW(),
        NOW(),
        'additional_document_detail',
        'Additional document detail',
        'DOCUMENTS',
        'We need one additional document to complete verification. Please upload the requested file from the Document Upload screen and reply here once it is ready.',
        TRUE
    ),
    (
        gen_random_uuid(),
        NOW(),
        NOW(),
        'approval_in_review',
        'Approval in review',
        'ONBOARDING',
        'Your company is currently in operational review. We will notify you as soon as the review is complete.',
        TRUE
    );
