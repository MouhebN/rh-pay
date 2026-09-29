CREATE TABLE chat_action_drafts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    leave_type VARCHAR(40),
    start_date DATE,
    end_date DATE,
    days INTEGER,
    reason VARCHAR(1000),
    leave_request_id BIGINT,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_chat_action_drafts_user UNIQUE (user_id),
    CONSTRAINT fk_chat_action_drafts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_action_drafts_updated_at ON chat_action_drafts(updated_at);
