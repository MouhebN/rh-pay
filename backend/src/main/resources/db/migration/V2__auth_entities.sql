ALTER TABLE users
    ADD COLUMN account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_login_at TIMESTAMP NULL,
    ADD COLUMN password_changed_at TIMESTAMP NULL;

UPDATE users
SET password_changed_at = COALESCE(password_changed_at, created_at);

ALTER TABLE users
    ALTER COLUMN password_changed_at SET NOT NULL;

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

CREATE TABLE login_audits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    ip_address VARCHAR(100) NULL,
    user_agent VARCHAR(500) NULL,
    failure_reason VARCHAR(500) NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_audits_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_login_audits_user_id ON login_audits(user_id);
CREATE INDEX idx_login_audits_username ON login_audits(username);
CREATE INDEX idx_login_audits_attempted_at ON login_audits(attempted_at);
