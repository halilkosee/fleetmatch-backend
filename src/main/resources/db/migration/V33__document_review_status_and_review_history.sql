ALTER TABLE company_documents
    ADD COLUMN review_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN review_notes VARCHAR(2000),
    ADD COLUMN reviewed_at TIMESTAMP,
    ADD COLUMN reviewed_by_user_id UUID;

CREATE TABLE company_review_events (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    actor_user_id UUID,
    action VARCHAR(100) NOT NULL,
    related_document_id UUID,
    reason VARCHAR(2000),
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_company_review_event_company
        FOREIGN KEY (company_id)
            REFERENCES companies(id),

    CONSTRAINT fk_company_review_event_actor
        FOREIGN KEY (actor_user_id)
            REFERENCES users(id)
);

CREATE INDEX idx_company_review_events_company_created
    ON company_review_events(company_id, created_at DESC);
