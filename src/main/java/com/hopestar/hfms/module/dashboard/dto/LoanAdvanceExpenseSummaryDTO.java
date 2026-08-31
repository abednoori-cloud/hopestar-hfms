package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Dashboard's Loans / Advances / Expenses cards. Loan and advance figures
 * are scoped to {@code ACTIVE} records only (mirroring {@link
 * OutstandingReceivablesDTO}'s {@code ACTIVE}-only scoping of student
 * contracts); the expense figure is the current calendar month's {@code
 * POSTED} total (mirroring {@link FinancialSummaryDTO}'s current-period
 * income/expense figures).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAdvanceExpenseSummaryDTO {

    private long activeLoanCount;
    private BigDecimal loanOutstandingUsd;

    private long activeAdvanceCount;
    private BigDecimal advanceOutstandingUsd;

    private BigDecimal currentPeriodExpenseUsd;

    /** e.g. "August 2026" -- the current-period label shown next to the expense figure. */
    private String currentPeriodLabel;
}
