CREATE TABLE companies (
                           id UUID PRIMARY KEY,
                           legal_name VARCHAR(255) NOT NULL,
                           dba_name VARCHAR(255),
                           email VARCHAR(255) NOT NULL,
                           phone VARCHAR(255),
                           type VARCHAR(50) NOT NULL,
                           verified BOOLEAN NOT NULL DEFAULT FALSE,
                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP NOT NULL
);

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       first_name VARCHAR(255) NOT NULL,
                       last_name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       phone VARCHAR(255),
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       carrier_user_type VARCHAR(50),
                       company_id UUID,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,

                       CONSTRAINT fk_users_company
                           FOREIGN KEY (company_id)
                               REFERENCES companies(id)
);