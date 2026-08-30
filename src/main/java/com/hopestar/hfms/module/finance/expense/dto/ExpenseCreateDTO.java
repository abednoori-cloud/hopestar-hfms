package com.hopestar.hfms.module.finance.expense.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
 * Input for creating a new (DRAFT) expense. {@code expenseNumber} and
 * {@code usdEquivalentAmount} are deliberately absent -- always
 * system-generated/computed by {@code ExpenseService}, per the approved
 * business rules; no caller ever supplies them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCreateDTO {

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotBlank(message = "Description is required")
    @Size(max = 500)
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    @Size(max = 2000)
    private String remarks;
}
