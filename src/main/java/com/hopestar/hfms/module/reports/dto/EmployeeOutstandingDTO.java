package com.hopestar.hfms.module.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One row of the Employee Report's active-loans/advances table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeOutstandingDTO {

    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;
    private BigDecimal loanOutstandingUsd;
    private BigDecimal advanceOutstandingUsd;
    private BigDecimal totalOutstandingUsd;
}
