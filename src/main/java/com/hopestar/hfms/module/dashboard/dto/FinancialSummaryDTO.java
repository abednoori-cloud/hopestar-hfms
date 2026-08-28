package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * All-time and current-month totals from the central ledger. Every amount
 * here is the already-stored {@code usdEquivalentAmount} of {@code POSTED}
 * transactions -- never a live-recalculated figure -- per the project's
 * "historical exchange rates are never recalculated" rule.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryDTO {

    private BigDecimal totalIncomeUsd;
    private BigDecimal totalExpenseUsd;
    private BigDecimal netPositionUsd;

    private BigDecimal currentPeriodIncomeUsd;
    private BigDecimal currentPeriodExpenseUsd;
    private BigDecimal currentPeriodNetUsd;

    /** e.g. "August 2026" -- the current-period label shown next to the current-period figures. */
    private String currentPeriodLabel;
}
