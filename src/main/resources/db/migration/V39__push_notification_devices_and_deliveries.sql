CREATE TABLE push_device_tokens (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    platform VARCHAR(50) NOT NULL,
    device_id VARCHAR(255),
    app_version VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_registered_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    CONSTRAINT fk_push_device_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_push_device_tokens_user_active
    ON push_device_tokens(user_id, active);

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    notification_id UUID NOT NULL,
    user_id UUID NOT NULL,
    device_token_id UUID,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    provider_message_id VARCHAR(255),
    error_message VARCHAR(1000),
    sent_at TIMESTAMP,
    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_notification_deliveries_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notification_deliveries_device_token
        FOREIGN KEY (device_token_id) REFERENCES push_device_tokens(id)
);

CREATE INDEX idx_notification_deliveries_notification
    ON notification_deliveries(notification_id);

CREATE INDEX idx_notification_deliveries_user_created
    ON notification_deliveries(user_id, created_at DESC);

CREATE INDEX idx_notification_deliveries_status
    ON notification_deliveries(status);
