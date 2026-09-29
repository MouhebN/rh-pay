ALTER TABLE leave_requests
    ADD COLUMN document_file_name VARCHAR(255),
    ADD COLUMN document_content_type VARCHAR(100),
    ADD COLUMN document_storage_path VARCHAR(500),
    ADD COLUMN document_size BIGINT,
    ADD COLUMN document_uploaded_at TIMESTAMP;
