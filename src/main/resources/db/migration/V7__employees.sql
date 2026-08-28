-- =====================================================================
-- V7__employees.sql
-- Employee Management Module (Phase 3C).
--
-- employee_statuses mirrors the student_statuses pattern established in
-- V3 (approved architecture §1.2.3): a database-driven lookup table
-- rather than a hardcoded enum, so the owner can add a new status later
-- as a data change, not a redeploy.
--
-- Currency handling mirrors student_contracts/student_payments exactly:
-- salary_currency + exchange_rate_to_usd + usd_equivalent_salary are
-- stored permanently side by side with the original amount, per the
-- project's base-currency rule (base = USD; USD forces rate 1; AFN/EUR
-- require a positive manually-entered rate).
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- employee_statuses (lookup)
-- ---------------------------------------------------------------------
CREATE TABLE employee_statuses (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(50)  NOT NULL,
    description       VARCHAR(255) NULL,

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NULL,
    created_by        VARCHAR(50)  NULL,
    updated_by        VARCHAR(50)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_employee_statuses_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- employees
-- ---------------------------------------------------------------------
CREATE TABLE employees (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code            VARCHAR(20)    NOT NULL,
    full_name                VARCHAR(150)   NOT NULL,
    father_name               VARCHAR(150)  NULL,
    gender                   VARCHAR(10)    NOT NULL,
    date_of_birth             DATE          NULL,
    phone                    VARCHAR(20)    NULL,
    email                    VARCHAR(100)   NULL,
    national_id              VARCHAR(50)    NULL,
    address                  VARCHAR(255)   NULL,
    position                 VARCHAR(100)   NOT NULL,
    department               VARCHAR(100)   NOT NULL,
    joining_date             DATE           NOT NULL,
    status_id                BIGINT         NOT NULL,
    base_salary              DECIMAL(12,2)  NOT NULL,
    salary_currency          VARCHAR(3)     NOT NULL DEFAULT 'USD',
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_salary    DECIMAL(12,2)  NOT NULL,
    notes                    VARCHAR(2000)  NULL,
    branch_id                BIGINT         NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_employees_employee_code UNIQUE (employee_code),
    -- NULL values do not collide under MySQL's unique-index semantics,
    -- so phone/email/national_id stay optional while still being unique
    -- whenever a value IS provided, matching the students table pattern.
    CONSTRAINT uk_employees_phone UNIQUE (phone),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT uk_employees_national_id UNIQUE (national_id),
    CONSTRAINT fk_employees_status
        FOREIGN KEY (status_id) REFERENCES employee_statuses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employees_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employees_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_employees_salary_currency CHECK (salary_currency IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_employees_base_salary_non_negative CHECK (base_salary >= 0),
    CONSTRAINT chk_employees_usd_equivalent_non_negative CHECK (usd_equivalent_salary >= 0),
    CONSTRAINT chk_employees_exchange_rate_positive CHECK (exchange_rate_to_usd > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_employees_status ON employees (status_id);
CREATE INDEX idx_employees_department ON employees (department);
CREATE INDEX idx_employees_position ON employees (position);
CREATE INDEX idx_employees_branch ON employees (branch_id);
CREATE INDEX idx_employees_full_name ON employees (full_name);

-- =====================================================================
-- SEED DATA
-- =====================================================================

INSERT INTO employee_statuses (name, description, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('ACTIVE',      'Currently employed and working',           TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('ON_LEAVE',    'Temporarily on approved leave',             TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('SUSPENDED',   'Employment temporarily suspended',          TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('RESIGNED',    'Voluntarily left the organization',         TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('TERMINATED',  'Employment ended by the organization',      TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');
