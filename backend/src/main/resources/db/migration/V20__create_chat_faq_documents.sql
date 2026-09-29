CREATE TABLE chat_faq_documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    category VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    storage_path VARCHAR(500) NOT NULL,
    extracted_text TEXT NOT NULL,
    uploaded_by_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_faq_documents_uploaded_by
        FOREIGN KEY (uploaded_by_id) REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_chat_faq_documents_created_at ON chat_faq_documents(created_at DESC);
