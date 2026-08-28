-- =====================================================================
-- V8__salaries.sql
-- Salary Management Module (Phase 3D).
--
-- Currency handling mirrors employees/student_contracts/student_payments
-- exactly: currency_code + exchange_rate_to_usd + usd_equivalent_salary
-- stored permanently alongside the original basic salary.
--
-- ledger_transaction_id is nullable: a DRAFT salary has not been posted
-- yet and therefore has no ledger entry -- created exclusively by
-- LedgerService.postExpense() once the salary is posted.
--
-- uk_salaries_employee_month_year enforces "one salary record per
-- employee per month/year" at the database level, in addition to the
-- service-layer duplicate check.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE salaries (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_number            VARCHAR(30)    NOT NULL,
    employee_id              BIGINT         NOT NULL,
    salary_month             INT            NOT NULL,
    salary_year              INT            NOT NULL,
    basic_salary             DECIMAL(12,2)  NOT NULL,
    currency_code            VARCHAR(3)     NOT NULL DEFAULT 'USD',
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_salary    DECIMAL(12,2)  NOT NULL,
    bonus                    DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    overtime_amount          DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    allowance                DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    penalty                  DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    loan_deduction           DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    advance_deduction        DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    other_deduction          DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    net_salary               DECIMAL(12,2)  NOT NULL,
    payment_status           VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    payment_date             DATE           NULL,
    payment_method_id        BIGINT         NOT NULL,
    ledger_transaction_id    BIGINT         NULL,
    remarks                  VARCHAR(2000)  NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_salaries_salary_number UNIQUE (salary_number),
    CONSTRAINT uk_salaries_employee_month_year UNIQUE (employee_id, salary_month, salary_year),
    CONSTRAINT fk_salaries_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT fk_salaries_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_salaries_ledger_transaction
        FOREIGN KEY (ledger_transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_salaries_month CHECK (salary_month BETWEEN 1 AND 12),
    CONSTRAINT chk_salaries_year CHECK (salary_year BETWEEN 2000 AND 2100),
    CONSTRAINT chk_salaries_status CHECK (payment_status IN ('DRAFT', 'POSTED', 'VOID')),
    CONSTRAINT chk_salaries_currency_code CHECK (currency_code IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_salaries_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    CONSTRAINT chk_salaries_amounts_non_negative CHECK (
        basic_salary >= 0 AND usd_equivalent_salary >= 0 AND bonus >= 0 AND overtime_amount >= 0
        AND allowance >= 0 AND penalty >= 0 AND loan_deduction >= 0 AND advance_deduction >= 0
        AND other_deduction >= 0 AND net_salary >= 0
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_salaries_employee ON salaries (employee_id);
CREATE INDEX idx_salaries_status ON salaries (payment_status);
CREATE INDEX idx_salaries_year_month ON salaries (salary_year, salary_month);
CREATE INDEX idx_salaries_payment_method ON salaries (payment_method_id);
