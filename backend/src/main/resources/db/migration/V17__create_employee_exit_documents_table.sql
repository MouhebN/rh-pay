CREATE TABLE employee_exit_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_exit_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    visible_to_employee BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by_id BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_exit_documents_exit FOREIGN KEY (employee_exit_id) REFERENCES employee_exits(id),
    CONSTRAINT fk_employee_exit_documents_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users(id)
);

CREATE INDEX idx_employee_exit_documents_exit_id ON employee_exit_documents(employee_exit_id);
CREATE INDEX idx_employee_exit_documents_type ON employee_exit_documents(type);
CREATE INDEX idx_employee_exit_documents_visible ON employee_exit_documents(visible_to_employee);
