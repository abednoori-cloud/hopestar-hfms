-- =====================================================================
-- V11__employee_advances.sql
-- Employee Advances Module.
--
-- employee_advances tracks the full lifecycle of a staff cash advance:
-- DRAFT (not yet disbursed) -> ACTIVE (disbursed, ledger_transaction_id
-- set via LedgerService.postExpense) -> CLOSED (remaining_balance
-- reached zero through repayments) or VOID (cancelled while DRAFT, or
-- disbursed then voided via LedgerService.voidTransaction). Mirrors
-- employee_loans exactly, minus the repayment-schedule-period fields
-- (monthly_deduction/start-end month/year) that don't apply to a
-- shorter-term cash advance with no fixed installment schedule.
--
-- advance_repayments is the auditable history behind
-- employee_advances.remaining_balance -- every repayment posts its own
-- ADVANCE_REPAYMENT income transaction via LedgerService.postIncome and
-- is never backed out by mutating remaining_balance directly outside
-- the service layer. Mirrors loan_repayments exactly.
--
-- Currency handling mirrors employee_loans/salaries exactly: currency_code
-- + exchange_rate_to_usd + usd_equivalent_amount stored permanently
-- alongside the original amount, on both tables independently.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- employee_advances
-- ---------------------------------------------------------------------
CREATE TABLE employee_advances (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    advance_number           VARCHAR(30)    NOT NULL,
    employee_id              BIGINT         NOT NULL,
    advance_amount           DECIMAL(12,2)  NOT NULL,
    currency_code            VARCHAR(3)     NOT NULL DEFAULT 'USD',
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_amount    DECIMAL(12,2)  NOT NULL,
    remaining_balance        DECIMAL(12,2)  NOT NULL,
    advance_date             DATE           NOT NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    remarks                  VARCHAR(2000)  NULL,
    ledger_transaction_id    BIGINT         NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_employee_advances_advance_number UNIQUE (advance_number),
    CONSTRAINT fk_employee_advances_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_advances_ledger_transaction
        FOREIGN KEY (ledger_transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_advances_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'VOID')),
    CONSTRAINT chk_employee_advances_currency_code CHECK (currency_code IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_employee_advances_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    CONSTRAINT chk_employee_advances_advance_amount_positive CHECK (advance_amount > 0),
    CONSTRAINT chk_employee_advances_remaining_balance_range CHECK (remaining_balance >= 0 AND remaining_balance <= advance_amount)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_employee_advances_employee ON employee_advances (employee_id);
CREATE INDEX idx_employee_advances_status ON employee_advances (status);

-- ---------------------------------------------------------------------
-- advance_repayments (auditable history behind employee_advances.remaining_balance)
-- ---------------------------------------------------------------------
CREATE TABLE advance_repayments (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    advance_id               BIGINT         NOT NULL,
    repayment_date           DATE           NOT NULL,
    amount                   DECIMAL(12,2)  NOT NULL,
    currency_code            VARCHAR(3)     NOT NULL DEFAULT 'USD',
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_amount    DECIMAL(12,2)  NOT NULL,
    payment_method_id        BIGINT         NOT NULL,
    ledger_transaction_id    BIGINT         NULL,
    notes                    VARCHAR(2000)  NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_advance_repayments_advance
        FOREIGN KEY (advance_id) REFERENCES employee_advances (id) ON DELETE RESTRICT,
    CONSTRAINT fk_advance_repayments_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_advance_repayments_ledger_transaction
        FOREIGN KEY (ledger_transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_advance_repayments_currency_code CHECK (currency_code IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_advance_repayments_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    CONSTRAINT chk_advance_repayments_amount_positive CHECK (amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_advance_repayments_advance ON advance_repayments (advance_id);
