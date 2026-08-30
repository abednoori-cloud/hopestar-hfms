package com.hopestar.hfms.module.finance.advance.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
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
 * Input for creating a new (DRAFT) employee advance. {@code
 * advanceNumber}, {@code usdEquivalentAmount}, and {@code
 * remainingBalance} are deliberately absent -- always system-generated/
 * computed by {@code EmployeeAdvanceService}, per the approved business
 * rules; no caller ever supplies them. Mirrors {@code
 * EmployeeLoanCreateDTO} minus the fixed monthly-deduction repayment
 * schedule (start/end month/year) a formal loan has.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAdvanceCreateDTO {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Advance amount is required")
    @Positive(message = "Advance amount must be greater than zero")
    private BigDecimal advanceAmount;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @NotNull(message = "Advance date is required")
    @PastOrPresent(message = "Advance date cannot be in the future")
    private LocalDate advanceDate;

    @Size(max = 2000)
    private String remarks;
}
