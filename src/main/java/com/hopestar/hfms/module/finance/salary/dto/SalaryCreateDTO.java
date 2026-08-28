package com.hopestar.hfms.module.finance.salary.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Input for creating a new (DRAFT) salary record. {@code salaryNumber},
 * {@code usdEquivalentSalary}, and {@code netSalary} are deliberately
 * absent — always system-generated/computed by {@code SalaryService},
 * per the approved business rules; no caller ever supplies them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryCreateDTO {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    private Integer year;

    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.0", message = "Basic salary cannot be negative")
    private BigDecimal basicSalary;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @DecimalMin(value = "0.0", message = "Bonus cannot be negative")
    private BigDecimal bonus = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Overtime amount cannot be negative")
    private BigDecimal overtimeAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Allowance cannot be negative")
    private BigDecimal allowance = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Penalty cannot be negative")
    private BigDecimal penalty = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Loan deduction cannot be negative")
    private BigDecimal loanDeduction = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Advance deduction cannot be negative")
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Other deduction cannot be negative")
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    @NotNull(message = "Payment method is required")
    private Long paymentMethodId;

    @Size(max = 2000)
    private String remarks;
}
