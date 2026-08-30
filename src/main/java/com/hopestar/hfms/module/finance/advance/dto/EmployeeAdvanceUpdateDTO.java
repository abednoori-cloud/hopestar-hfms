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
 * Input for editing a {@code DRAFT} employee advance. {@code employeeId}
 * is deliberately absent -- immutable once the advance is created,
 * matching how {@code EmployeeLoanUpdateDTO} omits its own immutable
 * fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAdvanceUpdateDTO {

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
