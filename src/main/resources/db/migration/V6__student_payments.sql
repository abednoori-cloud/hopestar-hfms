-- =====================================================================
-- V6__student_payments.sql
-- Student Payments Module (Phase 3B). Every POSTED payment has a
-- corresponding row in `transactions`, created exclusively by
-- LedgerService.postIncome() -- this table never posts to the ledger
-- itself. `transaction_id` is nullable because a DRAFT payment has not
-- been posted yet and therefore has no ledger entry.
--
-- `invoice_id` has no foreign key: the Invoice module does not exist yet
-- (a later phase). It is a plain nullable column today; a proper FK to
-- `invoices` can be added in that module's migration without touching
-- this table's other columns.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE student_payments (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_number           VARCHAR(30)    NOT NULL,
    receipt_number           VARCHAR(30)    NOT NULL,
    invoice_id               BIGINT         NULL,
    student_id               BIGINT         NOT NULL,
    contract_id              BIGINT         NOT NULL,
    transaction_id           BIGINT         NULL,
    payment_date             DATE           NOT NULL,
    original_amount          DECIMAL(12,2)  NOT NULL,
    currency_id              BIGINT         NOT NULL,
    exchange_rate_to_usd     DECIMAL(14,6)  NOT NULL DEFAULT 1.000000,
    usd_equivalent_amount    DECIMAL(12,2)  NOT NULL,
    payment_method_id        BIGINT         NOT NULL,
    reference_number         VARCHAR(100)   NULL,
    notes                    VARCHAR(2000)  NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'POSTED',

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by               VARCHAR(50)    NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_student_payments_payment_number UNIQUE (payment_number),
    CONSTRAINT uk_student_payments_receipt_number UNIQUE (receipt_number),
    CONSTRAINT fk_student_payments_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_payments_contract
        FOREIGN KEY (contract_id) REFERENCES student_contracts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_payments_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_payments_currency
        FOREIGN KEY (currency_id) REFERENCES currencies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_payments_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT chk_student_payments_status
        CHECK (status IN ('DRAFT', 'POSTED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_student_payments_amounts_non_negative
        CHECK (original_amount >= 0 AND usd_equivalent_amount >= 0),
    CONSTRAINT chk_student_payments_exchange_rate_positive
        CHECK (exchange_rate_to_usd > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_student_payments_student ON student_payments (student_id);
CREATE INDEX idx_student_payments_contract ON student_payments (contract_id);
CREATE INDEX idx_student_payments_status ON student_payments (status);
CREATE INDEX idx_student_payments_date ON student_payments (payment_date);
CREATE INDEX idx_student_payments_currency ON student_payments (currency_id);
CREATE INDEX idx_student_payments_payment_method ON student_payments (payment_method_id);
CREATE INDEX idx_student_payments_transaction ON student_payments (transaction_id);
