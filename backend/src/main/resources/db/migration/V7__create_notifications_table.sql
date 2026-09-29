CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL,
    link VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notifications_recipient_created_at ON notifications(recipient_id, created_at);
CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read);
