CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_positions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500) NULL,
    base_salary NUMERIC(12, 2) NULL,
    department_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_positions_department
        FOREIGN KEY (department_id) REFERENCES departments(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_job_positions_department_id ON job_positions(department_id);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NULL,
    birth_date DATE NULL,
    hire_date DATE NOT NULL,
    department_id BIGINT NULL,
    job_position_id BIGINT NULL,
    user_account_id BIGINT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id) REFERENCES departments(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_employees_job_position
        FOREIGN KEY (job_position_id) REFERENCES job_positions(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_employees_user_account
        FOREIGN KEY (user_account_id) REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_job_position_id ON employees(job_position_id);
CREATE INDEX idx_employees_status ON employees(status);
