CREATE TABLE conversations (
    id UUID PRIMARY KEY,

    load_id UUID NOT NULL UNIQUE,
    broker_company_id UUID NOT NULL,
    fleet_company_id UUID NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_conversations_load
        FOREIGN KEY (load_id)
        REFERENCES loads(id),

    CONSTRAINT fk_conversations_broker_company
        FOREIGN KEY (broker_company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_conversations_fleet_company
        FOREIGN KEY (fleet_company_id)
        REFERENCES companies(id)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,

    conversation_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,
    sender_company_id UUID NOT NULL,

    body VARCHAR(2000) NOT NULL,
    deleted_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id),

    CONSTRAINT fk_messages_sender_user
        FOREIGN KEY (sender_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_messages_sender_company
        FOREIGN KEY (sender_company_id)
        REFERENCES companies(id)
);

CREATE INDEX idx_conversations_broker_company
    ON conversations(broker_company_id);

CREATE INDEX idx_conversations_fleet_company
    ON conversations(fleet_company_id);

CREATE INDEX idx_messages_conversation_created_at
    ON messages(conversation_id, created_at);
