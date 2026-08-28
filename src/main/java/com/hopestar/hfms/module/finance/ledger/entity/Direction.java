package com.hopestar.hfms.module.finance.ledger.entity;

/**
 * Whether a transaction is money coming in or money going out. Reports
 * and the Dashboard (later phases) sum {@code usdEquivalentAmount} by
 * this field to compute income, expenses, and profit.
 */
public enum Direction {
    INCOME,
    EXPENSE
}
