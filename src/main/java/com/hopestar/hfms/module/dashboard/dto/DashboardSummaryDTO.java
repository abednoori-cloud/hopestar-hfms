package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Root view-model for the Dashboard page (Module 2), assembled by {@code
 * DashboardService} from the existing Ledger, Student Payment, Salary,
 * Student, and Employee modules -- read-only, per the approved
 * architecture's "report and dashboard modules are read-only consumers"
 * rule. Never exposes entities to the Thymeleaf layer directly.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    private FinancialSummaryDTO financialSummary;
    private OutstandingReceivablesDTO outstandingReceivables;
    private SalaryOverviewDTO salaryOverview;
    private List<RecentTransactionDTO> recentTransactions;
    private StudentEmployeeStatsDTO stats;
}
