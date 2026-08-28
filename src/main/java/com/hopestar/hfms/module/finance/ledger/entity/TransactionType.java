package com.hopestar.hfms.module.finance.ledger.entity;

/**
 * Every kind of financial event the system can post to the central
 * ledger ({@code transactions}), per the approved architecture's Finance
 * module design. Every current and future submodule (Student Payments,
 * English Test Payments, Salary, Loans, Advances, Expenses, Refunds,
 * Invoices) is represented here so {@link
 * com.hopestar.hfms.module.finance.ledger.service.LedgerService} never
 * needs a submodule-specific posting method — one {@code postIncome}/
 * {@code postExpense} pair, parameterized by this enum, covers all of
 * them.
 */
public enum TransactionType {
    STUDENT_PAYMENT,
    ENGLISH_TEST_PAYMENT,
    SALARY,
    EXPENSE,
    LOAN_DISBURSEMENT,
    LOAN_REPAYMENT,
    ADVANCE,
    ADVANCE_REPAYMENT,
    REFUND,
    INVOICE,
    OTHER
}
