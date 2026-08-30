-- =====================================================================
-- V12__expenses.sql
-- Expenses Module.
--
-- An expense is a simple office cost (rent, electricity, supplies, ...)
-- -- not a person-to-person transaction like employee_loans/
-- employee_advances, so there is no repayment/schedule concept at all:
-- an expense is posted once and that's it. Lifecycle: DRAFT (freely
-- editable) -> POSTED (posted to the ledger via LedgerService.postExpense,
-- ledger_transaction_id set) or VOID (cancelled while DRAFT with nothing
-- to unwind, or POSTED then voided via LedgerService.voidTransaction).
--
-- expense_categories is a small lookup table (Rent, Electricity, ...)
-- rather than a hardcoded enum, mirroring payment_methods' precedent so
-- the owner can add a category later as a data change, not a redeploy.
--
-- Currency handling mirrors employee_loans/employee_advances exactly:
-- currency_code + exchange_rate_to_usd + usd_equivalent_amount stored
-- permanently alongside the original amount.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- expense_categories
-- ---------------------------------------------------------------------
CREATE TABLE expense_categories (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                     VARCHAR(100)   NOT NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_expense_categories_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO expense_categories (name, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('Rent',              TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Electricity',       TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Internet',          TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Office Supplies',   TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Transportation',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Other',             TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');

-- ---------------------------------------------------------------------
-- expenses
-- ---------------------------------------------------------------------
CREATE TABLE expenses (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_number           VARCHAR(30)    NOT NULL,
    category_id              BIGINT         NOT NULL,
    description              VARCHAR(500)   NOT NULL,
    amount                   DECIMAL(12,2)  NOT NULL,
    currency_code            VARCHAR(3)     NOT NULL DEFAULT 'USD',
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_amount    DECIMAL(12,2)  NOT NULL,
    expense_date             DATE           NOT NULL,
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

    CONSTRAINT uk_expenses_expense_number UNIQUE (expense_number),
    CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id) REFERENCES expense_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_ledger_transaction
        FOREIGN KEY (ledger_transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_expenses_status CHECK (status IN ('DRAFT', 'POSTED', 'VOID')),
    CONSTRAINT chk_expenses_currency_code CHECK (currency_code IN ('USD', 'AFN', 'EUR')),
    CONSTRAINT chk_expenses_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_expenses_category ON expenses (category_id);
CREATE INDEX idx_expenses_status ON expenses (status);
CREATE INDEX idx_expenses_expense_date ON expenses (expense_date);
