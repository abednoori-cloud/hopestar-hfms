package com.hopestar.hfms.module.finance.expense.entity;

/**
 * Lifecycle status of an {@link Expense}: {@code DRAFT} (freely editable)
 * -&gt; {@code POSTED} (posted to the ledger, done -- an expense is a
 * one-time cost with no ongoing balance, unlike {@code EmployeeLoan}/
 * {@code EmployeeAdvance}) or {@code VOID} (cancelled).
 */
public enum ExpenseStatus {
    DRAFT,
    POSTED,
    VOID
}
