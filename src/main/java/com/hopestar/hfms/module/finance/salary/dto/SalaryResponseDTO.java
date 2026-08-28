package com.hopestar.hfms.module.finance.salary.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for a salary. Serves both the list page and the detail/view
 * page — one response shape, per the same precedent {@code
 * StudentResponseDTO}/{@code EmployeeResponseDTO} set in the Student and
 * Employee modules, rather than separate "List" and "View" DTOs with
 * identical fields.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryResponseDTO extends BaseAuditDTO {

    private String salaryNumber;

    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;
    private String employeeDepartment;

    private Integer month;
    private Integer year;

    private BigDecimal basicSalary;
    private SupportedCurrency currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentSalary;

    private BigDecimal bonus;
    private BigDecimal overtimeAmount;
    private BigDecimal allowance;
    private BigDecimal penalty;
    private BigDecimal loanDeduction;
    private BigDecimal advanceDeduction;
    private BigDecimal otherDeduction;
    private BigDecimal netSalary;

    private SalaryPaymentStatus paymentStatus;
    private LocalDate paymentDate;
    private PaymentMethodResponseDTO paymentMethod;

    private Long ledgerTransactionId;
    private String ledgerTransactionCode;

    private String remarks;
}
