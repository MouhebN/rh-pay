CREATE TABLE payslips (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    payroll_calculation_id BIGINT NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT,
    base_salary NUMERIC(12,2) NOT NULL,
    gross_salary NUMERIC(12,2) NOT NULL,
    total_deductions NUMERIC(12,2) NOT NULL,
    net_salary NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    generated_by_id BIGINT,
    generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payslips_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_payslips_payroll_calculation FOREIGN KEY (payroll_calculation_id) REFERENCES payroll_calculations(id),
    CONSTRAINT fk_payslips_generated_by FOREIGN KEY (generated_by_id) REFERENCES users(id),
    CONSTRAINT uk_payslips_employee_month_year UNIQUE (employee_id, month, year),
    CONSTRAINT chk_payslips_month CHECK (month >= 1 AND month <= 12),
    CONSTRAINT chk_payslips_year CHECK (year >= 2020 AND year <= 2100)
);

CREATE INDEX idx_payslips_period ON payslips(year, month);
CREATE INDEX idx_payslips_status ON payslips(status);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);
