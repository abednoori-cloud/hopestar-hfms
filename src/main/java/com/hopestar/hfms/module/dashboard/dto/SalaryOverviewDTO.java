package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard's salary-overview widget. {@link #pendingBasicPayUsd} is
 * deliberately labeled "basic pay," not "net salary" -- only {@code
 * Salary#usdEquivalentSalary} (the basic-salary component) has a stored
 * USD conversion; bonus/overtime/allowance/deductions that make up net
 * salary are never converted to USD, so summing net salary across
 * employees paid in different currencies would silently mix currencies.
 * See the Module 2 final report for this limitation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryOverviewDTO {

    private long pendingSalaryCount;
    private BigDecimal pendingBasicPayUsd;

    private long currentPeriodTotalCount;
    private long currentPeriodDraftCount;
    private long currentPeriodPostedCount;
    private String currentPeriodLabel;

    private List<PendingSalaryDTO> pendingSalaries;
}
