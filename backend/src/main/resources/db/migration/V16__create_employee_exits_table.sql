CREATE TABLE employee_exits (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    exit_date DATE NOT NULL,
    reason TEXT NOT NULL,
    notes TEXT,
    account_disabled BOOLEAN NOT NULL DEFAULT FALSE,
    active_contract_terminated BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_id BIGINT,
    completed_by_id BIGINT,
    completed_at TIMESTAMP,
    cancelled_by_id BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_employee_exits_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_employee_exits_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT fk_employee_exits_completed_by FOREIGN KEY (completed_by_id) REFERENCES users(id),
    CONSTRAINT fk_employee_exits_cancelled_by FOREIGN KEY (cancelled_by_id) REFERENCES users(id)
);

CREATE INDEX idx_employee_exits_employee_id ON employee_exits(employee_id);
CREATE INDEX idx_employee_exits_status ON employee_exits(status);
CREATE INDEX idx_employee_exits_type ON employee_exits(type);
CREATE INDEX idx_employee_exits_exit_date ON employee_exits(exit_date);
