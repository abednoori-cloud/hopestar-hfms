package com.hopestar.hfms.module.finance.studentpayment.entity;

/**
 * Lifecycle status of a {@link StudentPayment}. Only {@code POSTED}
 * payments affect a contract's remaining balance (per the approved
 * business rules) and only a {@code POSTED} payment has a corresponding
 * {@code transactions} row. {@code REFUNDED} is declared now so the
 * schema does not need to change when the Refund module (a later phase)
 * is built, but no refund workflow is implemented in this phase.
 */
public enum PaymentStatus {
    DRAFT,
    POSTED,
    CANCELLED,
    REFUNDED
}
