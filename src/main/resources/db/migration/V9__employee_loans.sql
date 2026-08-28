-- =====================================================================
-- V9__employee_loans.sql
-- Employee Loans Module (Phase 3E).
--
-- employee_loans tracks the full lifecycle of a staff loan: DRAFT (not
-- yet disbursed) -> ACTIVE (disbursed, ledger_transaction_id set via
-- LedgerService.postExpense) -> CLOSED (remaining_balance reached zero
-- through repayments) or VOID (cancelled while DRAFT, or disbursed then
-- voided via LedgerService.voidTransaction).
--
-- loan_repayments is the auditable history behind
-- employee_loans.remaining_balance -- every repayment posts its own
-- LOAN_REPAYMENT income transaction via LedgerService.postIncome and is
-- never backed out by mutating remaining_balance directly outside the
-- service layer.
--
-- Currency handling mirrors employees/salaries exactly: currency_code +
-- exchange_rate_to_usd + usd_equivalent_amount stored permanently
-- alongside the original amount, on both tables independently (a loan
-- and its repayments may in principle use different currencies/rates,
-- exactly like a student_contract and its student_payments).
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- employee_loans
-- ---------------------------------------------------------------------
CREATE TABLE employee_loans (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_number              VARCHAR(30)    NOT NULL,
    employee_id              BIGINT         NOT NULL,
    loan_amount              DECIMAL(12,2)  NOT NULL,
    currency_code            VARCHAR(3)     NOT NULL DEFAULT 'USD',
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_amount    DECIMAL(12,2)  NOT NULL,
    monthly_deduction        DECIMAL(12,2)  NOT NULL,
    remaining_balance        DECIMAL(12,2)  NOT NULL,
    loan_date                DATE           NOT NULL,
    start_month              INT            NOT NULL,
    start_year               INT            NOT NULL,
    end_month                INT            NOT NULL,
    end_year                 INT            NOT NULL,
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

    CONSTRAINT uk_employee_loans_loan_number UNIQUE (loan_number),
    CONSTRAINT fk_employee_loans_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_loans_ledger_transaction
        FOREIGN KEY (ledger_transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_loans_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'VOID')),
    CONSTRAINT chk_employee_loans_currency_code CHECK (currency_code IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_employee_loans_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    CONSTRAINT chk_employee_loans_loan_amount_positive CHECK (loan_amount > 0),
    CONSTRAINT chk_employee_loans_monthly_deduction_positive CHECK (monthly_deduction > 0),
    CONSTRAINT chk_employee_loans_monthly_deduction_le_amount CHECK (monthly_deduction <= loan_amount),
    CONSTRAINT chk_employee_loans_remaining_balance_range CHECK (remaining_balance >= 0 AND remaining_balance <= loan_amount),
    CONSTRAINT chk_employee_loans_start_month CHECK (start_month BETWEEN 1 AND 12),
    CONSTRAINT chk_employee_loans_end_month CHECK (end_month BETWEEN 1 AND 12)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_employee_loans_employee ON employee_loans (employee_id);
CREATE INDEX idx_employee_loans_status ON employee_loans (status);

-- ---------------------------------------------------------------------
-- loan_repayments (auditable history behind employee_loans.remaining_balance)
-- ---------------------------------------------------------------------
CREATE TABLE loan_repayments (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id                  BIGINT         NOT NULL,
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

    CONSTRAINT fk_loan_repayments_loan
        FOREIGN KEY (loan_id) REFERENCES employee_loans (id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_repayments_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_repayments_ledger_transaction
        FOREIGN KEY (ledger_transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_loan_repayments_currency_code CHECK (currency_code IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_loan_repayments_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    CONSTRAINT chk_loan_repayments_amount_positive CHECK (amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_loan_repayments_loan ON loan_repayments (loan_id);
