package com.hopestar.hfms.module.finance.salary.entity;

/**
 * Lifecycle status of a {@link Salary} record. Named distinctly from
 * {@code studentpayment.entity.PaymentStatus} (a different value set --
 * DRAFT/POSTED/CANCELLED/REFUNDED there vs DRAFT/POSTED/VOID here) rather
 * than reused, since the two represent genuinely different domain
 * workflows; reusing one for the other would force an artificial mapping
 * between unrelated states.
 * <p>
 * Only {@code POSTED} salaries have a corresponding {@code transactions}
 * row, created exclusively by {@code LedgerService.postExpense}. A salary
 * is editable only while {@code DRAFT}; it becomes read-only once
 * {@code POSTED}, per the approved business rules.
 */
public enum SalaryPaymentStatus {
    DRAFT,
    POSTED,
    VOID
}
