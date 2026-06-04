CREATE TABLE company_documents
(
    id UUID PRIMARY KEY,

    company_id UUID NOT NULL,

    document_type VARCHAR(50) NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    file_url VARCHAR(500) NOT NULL,

    uploaded_at TIMESTAMP,

    CONSTRAINT fk_company_document_company
        FOREIGN KEY (company_id)
            REFERENCES companies(id)
);