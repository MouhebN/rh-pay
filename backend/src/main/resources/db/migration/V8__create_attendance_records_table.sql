CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    worked_hours NUMERIC(5, 2),
    late_minutes INTEGER,
    overtime_minutes INTEGER,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    admin_note TEXT,
    adjusted_by_id BIGINT,
    adjusted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_attendance_records_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_attendance_records_adjusted_by
        FOREIGN KEY (adjusted_by_id) REFERENCES users(id),
    CONSTRAINT uk_attendance_records_employee_date
        UNIQUE (employee_id, work_date)
);

CREATE INDEX idx_attendance_records_work_date ON attendance_records(work_date);
CREATE INDEX idx_attendance_records_status ON attendance_records(status);
CREATE INDEX idx_attendance_records_source ON attendance_records(source);
