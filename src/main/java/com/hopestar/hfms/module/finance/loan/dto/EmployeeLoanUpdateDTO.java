package com.hopestar.hfms.module.finance.loan.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input for editing a {@code DRAFT} employee loan. {@code employeeId} is
 * deliberately absent -- immutable once the loan is created, matching
 * how {@code SalaryUpdateDTO} omits its own immutable fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLoanUpdateDTO {

    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be greater than zero")
    private BigDecimal loanAmount;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @NotNull(message = "Monthly deduction is required")
    @Positive(message = "Monthly deduction must be greater than zero")
    private BigDecimal monthlyDeduction;

    @NotNull(message = "Loan date is required")
    @PastOrPresent(message = "Loan date cannot be in the future")
    private LocalDate loanDate;

    @NotNull(message = "Start month is required")
    @Min(value = 1, message = "Start month must be between 1 and 12")
    @Max(value = 12, message = "Start month must be between 1 and 12")
    private Integer startMonth;

    @NotNull(message = "Start year is required")
    private Integer startYear;

    @NotNull(message = "End month is required")
    @Min(value = 1, message = "End month must be between 1 and 12")
    @Max(value = 12, message = "End month must be between 1 and 12")
    private Integer endMonth;

    @NotNull(message = "End year is required")
    private Integer endYear;

    @Size(max = 2000)
    private String remarks;
}
