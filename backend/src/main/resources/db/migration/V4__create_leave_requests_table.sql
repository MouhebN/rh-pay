CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DOUBLE PRECISION NOT NULL,
    reason VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    reviewed_by_id BIGINT,
    reviewed_at TIMESTAMP,
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_leave_requests_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_requests_reviewed_by
        FOREIGN KEY (reviewed_by_id) REFERENCES users(id)
);

CREATE INDEX idx_leave_requests_employee_id ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_type ON leave_requests(type);
CREATE INDEX idx_leave_requests_dates ON leave_requests(start_date, end_date);
