CREATE TABLE contracts (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    trial_period_end_date DATE,
    salary_at_signature NUMERIC(12,2) NOT NULL,
    contract_reference VARCHAR(100) UNIQUE,
    notes TEXT,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    terminated_by_id BIGINT,
    termination_date DATE,
    termination_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_contracts_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_contracts_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT fk_contracts_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id),
    CONSTRAINT fk_contracts_terminated_by FOREIGN KEY (terminated_by_id) REFERENCES users(id),
    CONSTRAINT chk_contracts_salary_positive CHECK (salary_at_signature >= 0),
    CONSTRAINT chk_contracts_dates CHECK (end_date IS NULL OR start_date <= end_date),
    CONSTRAINT chk_contracts_trial_start CHECK (trial_period_end_date IS NULL OR trial_period_end_date >= start_date),
    CONSTRAINT chk_contracts_trial_end CHECK (trial_period_end_date IS NULL OR end_date IS NULL OR trial_period_end_date <= end_date)
);

CREATE INDEX idx_contracts_employee_id ON contracts(employee_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_type ON contracts(type);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);
