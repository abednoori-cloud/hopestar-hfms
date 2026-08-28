package com.hopestar.hfms.module.finance.ledger.entity;

/**
 * Lifecycle status of a ledger transaction. {@code VOIDED} is the
 * business-level "cancel" operation — per the approved Business Rules
 * ("financial records cannot be permanently deleted; cancelled records
 * remain in history"), a voided transaction stays visible in the ledger
 * with {@code active = true}; only its status changes. This is distinct
 * from {@link com.hopestar.hfms.common.entity.BaseEntity}'s soft-delete
 * flag, which is reserved for correcting a genuine data-entry mistake
 * rather than the routine act of cancelling a transaction.
 */
public enum TransactionStatus {
    DRAFT,
    POSTED,
    VOIDED
}
