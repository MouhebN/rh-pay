CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    reply VARCHAR(4000) NOT NULL,
    intent VARCHAR(40) NOT NULL,
    link VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_messages_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_user_created_at ON chat_messages(user_id, created_at);
