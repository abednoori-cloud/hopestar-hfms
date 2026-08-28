package com.hopestar.hfms.module.finance.ledger.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The one and only way any module posts a financial event to the ledger —
 * passed to {@link com.hopestar.hfms.module.finance.ledger.service.LedgerService#postIncome}
 * or {@code #postExpense}. Direction is deliberately not a field here: it
 * is implied by which of those two methods the caller invokes, so a
 * caller can never mismatch a type against the wrong direction.
 * <p>
 * {@code usdEquivalentAmount} is deliberately absent — {@code
 * LedgerServiceImpl} always computes it from {@code originalAmount} and
 * {@code exchangeRateToUsd}, per the project's currency rule; no caller,
 * client-side or otherwise, is ever trusted to supply it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Original amount is required")
    @DecimalMin(value = "0.0", message = "Original amount cannot be negative")
    private BigDecimal originalAmount;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currencyCode;

    /**
     * Required (and must be positive) whenever {@link #currencyCode} is
     * not USD. Ignored for USD — {@code LedgerServiceImpl} always forces
     * exactly {@code 1.000000} for USD, per the project's currency rule,
     * regardless of what is supplied here.
     */
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @NotNull(message = "Payment method is required")
    private Long paymentMethodId;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    /** Polymorphic link back to the originating record's table, e.g. {@code "student_contracts"}. */
    @Size(max = 100)
    private String referenceTable;

    /** The id of the originating record within {@link #referenceTable}. */
    private Long referenceId;

    /** Defaults to the headquarters branch (see {@code SequenceGeneratorService}'s branch resolution) when omitted. */
    private Long branchId;

    @Size(max = 2000)
    private String notes;
}
