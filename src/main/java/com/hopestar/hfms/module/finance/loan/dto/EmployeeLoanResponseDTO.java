package com.hopestar.hfms.module.finance.loan.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for an employee loan. Serves both the list page and the
 * detail/view page -- one response shape, per the same precedent {@code
 * SalaryResponseDTO}/{@code EmployeeResponseDTO} set elsewhere, rather
 * than separate "List" and "View" DTOs with identical fields.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLoanResponseDTO extends BaseAuditDTO {

    private String loanNumber;

    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;

    private BigDecimal loanAmount;
    private SupportedCurrency currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;

    private BigDecimal monthlyDeduction;
    private BigDecimal remainingBalance;

    private LocalDate loanDate;
    private Integer startMonth;
    private Integer startYear;
    private Integer endMonth;
    private Integer endYear;

    private LoanStatus status;
    private String remarks;

    private Long ledgerTransactionId;
    private String ledgerTransactionCode;
}
