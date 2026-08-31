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
 * Employee Report: total payroll cost for a date range, plus every
 * employee currently carrying an ACTIVE loan and/or advance balance
 * (current-state, not date-filtered -- same distinction as the Student
 * Report).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeReportResponseDTO {

    private LocalDate from;
    private LocalDate to;

    private BigDecimal totalPayrollCostUsd;
    private long payrollTransactionCount;

    private BigDecimal totalEmployeeOutstandingUsd;
    private List<EmployeeOutstandingDTO> employeesWithBalances;
}
