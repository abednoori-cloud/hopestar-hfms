package com.hopestar.hfms.module.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Money In/Out report: total income vs. expense (from {@code POSTED}
 * {@code Transaction} rows only, per the project's single-ledger-source-
 * of-truth rule) for a date range, plus a per-type breakdown.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowReportResponseDTO {

    private LocalDate from;
    private LocalDate to;

    private BigDecimal totalIncomeUsd;
    private BigDecimal totalExpenseUsd;
    private BigDecimal netPositionUsd;

    private List<TransactionTypeBreakdownDTO> breakdown;
}
