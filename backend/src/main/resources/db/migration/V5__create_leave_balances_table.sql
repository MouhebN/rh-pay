CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    acquired_days DOUBLE PRECISION NOT NULL,
    consumed_days DOUBLE PRECISION NOT NULL,
    pending_days DOUBLE PRECISION NOT NULL,
    remaining_days DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_leave_balances_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uk_leave_balances_employee_year
        UNIQUE (employee_id, year)
);

CREATE INDEX idx_leave_balances_year ON leave_balances(year);
