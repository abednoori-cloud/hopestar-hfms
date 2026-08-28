package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.module.finance.ledger.dto.PostTransactionRequest;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;

/**
 * The single entry point for posting money movement to the central
 * ledger ({@code transactions}). Per the approved Finance module
 * architecture, every current and future submodule (Student Payments,
 * English Test Payments, Salary, Loans, Advances, Expenses, Refunds,
 * Invoices) calls one of the methods below instead of ever constructing
 * or persisting a {@link com.hopestar.hfms.module.finance.ledger.entity.Transaction}
 * itself. This is what guarantees the Dashboard and Reports (later
 * phases) always reconcile against one consistent source of truth.
 * <p>
 * All currency-conversion business rules (USD forces rate {@code 1};
 * AFN/EUR require a positive manually-entered rate; {@code
 * usdEquivalentAmount} is always computed here, never trusted from a
 * caller) are enforced exclusively in {@link LedgerServiceImpl} — no
 * other class performs this calculation.
 */
public interface LedgerService {

    /** Posts an income transaction (e.g. a student payment). */
    TransactionResponseDTO postIncome(PostTransactionRequest request);

    /** Posts an expense transaction (e.g. an office expense, a salary payout). */
    TransactionResponseDTO postExpense(PostTransactionRequest request);

    /**
     * Voids a posted transaction. Per the approved business rules,
     * financial records are never permanently deleted — this changes the
     * transaction's status to {@code VOIDED} and keeps it fully visible
     * in the ledger's history; {@code reason}, if supplied, is appended
     * to the transaction's notes.
     */
    TransactionResponseDTO voidTransaction(Long transactionId, String reason);

    /** Looks up a single transaction by its surrogate id. */
    TransactionResponseDTO findTransaction(Long transactionId);

    /** Looks up a single transaction by its system-generated code, e.g. {@code TXN-2026-000001}. */
    TransactionResponseDTO findTransactionByCode(String transactionCode);
}
