package com.hopestar.hfms.module.finance.loan.entity;

/**
 * Lifecycle status of an {@link EmployeeLoan}: {@code DRAFT} (not yet
 * disbursed, freely editable) -&gt; {@code ACTIVE} (disbursed, ledger
 * transaction posted) -&gt; {@code CLOSED} (repaid in full) or {@code
 * VOID} (cancelled).
 */
public enum LoanStatus {
    DRAFT,
    ACTIVE,
    CLOSED,
    VOID
}
