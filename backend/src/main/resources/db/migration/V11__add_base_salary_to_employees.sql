ALTER TABLE employees
    ADD COLUMN base_salary NUMERIC(12,2);

UPDATE employees e
SET base_salary = jp.base_salary
FROM job_positions jp
WHERE e.job_position_id = jp.id
  AND e.base_salary IS NULL;
