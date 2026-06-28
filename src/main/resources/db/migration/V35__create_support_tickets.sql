CREATE TABLE support_tickets (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    company_id UUID,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    status VARCHAR(50) NOT NULL,
    admin_reply VARCHAR(4000),
    expected_response_at TIMESTAMP,
    answered_at TIMESTAMP,
    CONSTRAINT fk_support_ticket_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_support_ticket_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE INDEX idx_support_tickets_user_created
    ON support_tickets(user_id, created_at DESC);

CREATE INDEX idx_support_tickets_status_created
    ON support_tickets(status, created_at ASC);
