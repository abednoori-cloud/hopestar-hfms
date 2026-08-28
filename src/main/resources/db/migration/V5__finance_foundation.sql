-- =====================================================================
-- V5__finance_foundation.sql
-- Finance Foundation (Phase 3A) -- the central ledger that every future
-- financial submodule (Student Payments, English Test Payments, Salary,
-- Loans, Advances, Expenses, Refunds, Invoices) will post through via
-- LedgerService. No submodule ever writes to `transactions` directly.
--
-- Currency rule (project-wide, established with Student Management's
-- V4 migration and now formalized as a real master table):
--   * Base currency = USD.
--   * Supported currencies = USD, AFN, EUR.
--   * Exchange rates are entered manually by the user -- no live FX feed.
--   * Every financial record stores original_amount, currency (FK),
--     exchange_rate_to_usd, and usd_equivalent_amount, permanently.
--     Historical rates are never rewritten by a later exchange-rate
--     change; each transaction is its own historical snapshot.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- currencies
-- ---------------------------------------------------------------------
CREATE TABLE currencies (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(3)   NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    symbol              VARCHAR(5)   NOT NULL,
    is_base_currency    BOOLEAN      NOT NULL DEFAULT FALSE,

    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at          DATETIME     NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NULL,
    created_by          VARCHAR(50)  NULL,
    updated_by          VARCHAR(50)  NULL,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_currencies_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- payment_methods
-- ---------------------------------------------------------------------
CREATE TABLE payment_methods (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(50)  NOT NULL,
    code                VARCHAR(20)  NULL,
    description         VARCHAR(255) NULL,

    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at          DATETIME     NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NULL,
    created_by          VARCHAR(50)  NULL,
    updated_by          VARCHAR(50)  NULL,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_payment_methods_name UNIQUE (name),
    CONSTRAINT uk_payment_methods_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- transactions (the central ledger)
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_code         VARCHAR(30)    NOT NULL,
    transaction_type         VARCHAR(30)    NOT NULL,
    direction                VARCHAR(10)    NOT NULL,
    original_amount          DECIMAL(12,2)  NOT NULL,
    currency_id              BIGINT         NOT NULL,
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_amount    DECIMAL(12,2)  NOT NULL,
    payment_method_id        BIGINT         NOT NULL,
    transaction_date         DATE           NOT NULL,
    reference_table          VARCHAR(100)   NULL,
    reference_id             BIGINT         NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'POSTED',
    notes                    VARCHAR(2000)  NULL,
    branch_id                BIGINT         NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_transactions_code UNIQUE (transaction_code),
    CONSTRAINT fk_transactions_currency
        FOREIGN KEY (currency_id) REFERENCES currencies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_direction CHECK (direction IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_transactions_status CHECK (status IN ('DRAFT', 'POSTED', 'VOIDED')),
    CONSTRAINT chk_transactions_type CHECK (transaction_type IN (
        'STUDENT_PAYMENT', 'ENGLISH_TEST_PAYMENT', 'SALARY', 'EXPENSE',
        'LOAN_DISBURSEMENT', 'LOAN_REPAYMENT', 'ADVANCE', 'ADVANCE_REPAYMENT',
        'REFUND', 'INVOICE', 'OTHER'
    )),
    CONSTRAINT chk_transactions_amounts_non_negative
        CHECK (original_amount >= 0 AND usd_equivalent_amount >= 0),
    CONSTRAINT chk_transactions_exchange_rate_positive
        CHECK (exchange_rate_to_usd > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_transactions_type ON transactions (transaction_type);
CREATE INDEX idx_transactions_direction ON transactions (direction);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_date ON transactions (transaction_date);
CREATE INDEX idx_transactions_branch ON transactions (branch_id);
CREATE INDEX idx_transactions_currency ON transactions (currency_id);
CREATE INDEX idx_transactions_payment_method ON transactions (payment_method_id);
CREATE INDEX idx_transactions_reference ON transactions (reference_table, reference_id);

-- =====================================================================
-- SEED DATA
-- =====================================================================

INSERT INTO currencies (code, name, symbol, is_base_currency, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('USD', 'US Dollar',       '$', TRUE,  TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('AFN', 'Afghan Afghani',  '؋', FALSE, TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('EUR', 'Euro',            '€', FALSE, TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');

INSERT INTO payment_methods (name, code, description, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('Cash',           'CASH',     'Physical cash payment',                      TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Bank Transfer',  'BANK',     'Direct bank transfer',                       TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Credit Card',    'CREDIT',   'Credit card payment',                        TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Debit Card',     'DEBIT',    'Debit card payment',                         TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Mobile Wallet',  'MOBILE',   'Mobile money / e-wallet payment',            TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Cheque',         'CHEQUE',   'Bank cheque payment',                        TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Other',          'OTHER',    'Any payment method not covered above',       TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');
