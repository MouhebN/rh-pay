CREATE TABLE payroll_variables (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    unpaid_absence_days NUMERIC(5,2) NOT NULL DEFAULT 0,
    paid_leave_days NUMERIC(5,2) NOT NULL DEFAULT 0,
    sick_leave_days NUMERIC(5,2) NOT NULL DEFAULT 0,
    late_minutes INTEGER NOT NULL DEFAULT 0,
    overtime_minutes INTEGER NOT NULL DEFAULT 0,
    worked_days NUMERIC(5,2) NOT NULL DEFAULT 0,
    absent_days NUMERIC(5,2) NOT NULL DEFAULT 0,
    bonus_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    advance_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    notes TEXT,
    status VARCHAR(30) NOT NULL,
    generated_by_id BIGINT,
    generated_at TIMESTAMP,
    validated_by_id BIGINT,
    validated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payroll_variables_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_payroll_variables_generated_by
        FOREIGN KEY (generated_by_id) REFERENCES users(id),
    CONSTRAINT fk_payroll_variables_validated_by
        FOREIGN KEY (validated_by_id) REFERENCES users(id),
    CONSTRAINT uk_payroll_variables_employee_month_year
        UNIQUE (employee_id, month, year),
    CONSTRAINT chk_payroll_variables_month
        CHECK (month >= 1 AND month <= 12),
    CONSTRAINT chk_payroll_variables_year
        CHECK (year >= 2020 AND year <= 2100)
);

CREATE INDEX idx_payroll_variables_period ON payroll_variables(year, month);
CREATE INDEX idx_payroll_variables_status ON payroll_variables(status);
CREATE INDEX idx_payroll_variables_employee ON payroll_variables(employee_id);
