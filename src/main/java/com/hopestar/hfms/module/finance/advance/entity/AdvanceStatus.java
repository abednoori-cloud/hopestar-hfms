package com.hopestar.hfms.module.finance.advance.entity;

/**
 * Lifecycle status of an {@link EmployeeAdvance}: {@code DRAFT} (not yet
 * disbursed, freely editable) -&gt; {@code ACTIVE} (disbursed, ledger
 * transaction posted) -&gt; {@code CLOSED} (repaid in full) or {@code
 * VOID} (cancelled). Mirrors {@code LoanStatus} exactly.
 */
public enum AdvanceStatus {
    DRAFT,
    ACTIVE,
    CLOSED,
    VOID
}
