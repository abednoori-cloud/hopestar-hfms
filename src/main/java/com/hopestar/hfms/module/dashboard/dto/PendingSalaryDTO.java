package com.hopestar.hfms.module.dashboard.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One DRAFT (unpaid) salary record, for the Dashboard's salary-overview widget. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingSalaryDTO {

    private Long salaryId;
    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;
    private Integer month;
    private Integer year;
    private BigDecimal netSalary;
    private SupportedCurrency currency;
}
