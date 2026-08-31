-- =====================================================================
-- V13__remove_invoice_scaffolding.sql
-- Removes dead scaffolding for the Invoice module, which was never built
-- (HopeStar's actual workflow is Receipt/Voucher PDFs over the existing
-- StudentPayment/Expense records, not a separate invoice-demands-payment
-- concept -- see the Receipt/Voucher inspection notes).
--
-- Verified before writing this migration (against the dev database):
--   SELECT COUNT(*) FROM transactions WHERE transaction_type = 'INVOICE'      -> 0
--   SELECT COUNT(*) FROM student_payments WHERE invoice_id IS NOT NULL        -> 0
-- Both are zero because no code path has ever set either value: `invoice_id`
-- has never had a form field, and `TransactionType.INVOICE` was never used
-- by any submodule's ledger-posting call. Dropping them loses no data.
-- =====================================================================

SET NAMES utf8mb4;

-- student_payments.invoice_id: plain nullable column, no foreign key,
-- never set by any code path (see V6__student_payments.sql's own comment
-- on why it was added with no FK in the first place).
ALTER TABLE student_payments DROP COLUMN invoice_id;

-- transactions.transaction_type CHECK constraint: drop and recreate
-- without 'INVOICE'. TransactionType.INVOICE (Java enum) is removed in
-- the same change.
ALTER TABLE transactions DROP CHECK chk_transactions_type;
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_type CHECK (transaction_type IN (
    'STUDENT_PAYMENT', 'ENGLISH_TEST_PAYMENT', 'SALARY', 'EXPENSE',
    'LOAN_DISBURSEMENT', 'LOAN_REPAYMENT', 'ADVANCE', 'ADVANCE_REPAYMENT',
    'REFUND', 'OTHER'
));

-- Seeded permissions (V1__init.sql): INVOICE_VIEW is repurposed for the
-- new Receipt/Voucher PDF feature -- it is not yet enforced by any
-- @PreAuthorize (viewing a receipt/voucher currently only requires being
-- an authenticated user, matching the existing StudentPayment/Expense
-- view routes), but is seeded now in case per-permission gating is added
-- later, per the same "granular enough for future roles" rationale
-- V1__init.sql already used for every other permission.
--
-- INVOICE_MANAGE has no corresponding action to repurpose -- a receipt/
-- voucher is a read-only PDF derived from an existing record, there is
-- nothing to "manage" -- so it is removed outright (its role_permissions
-- row first, to satisfy fk_role_permissions_permission's ON DELETE
-- RESTRICT).
UPDATE permissions
SET code = 'RECEIPT_VIEW',
    description = 'View/download payment receipts and expense vouchers',
    module = 'FINANCE',
    updated_at = UTC_TIMESTAMP(),
    updated_by = 'system'
WHERE code = 'INVOICE_VIEW';

DELETE FROM role_permissions
WHERE permission_id = (SELECT id FROM permissions WHERE code = 'INVOICE_MANAGE');

DELETE FROM permissions WHERE code = 'INVOICE_MANAGE';
