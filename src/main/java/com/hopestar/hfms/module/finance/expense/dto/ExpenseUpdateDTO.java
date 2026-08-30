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
 * Input for editing a {@code DRAFT} expense. Unlike {@code
 * EmployeeAdvanceUpdateDTO} (which omits {@code employeeId} as an
 * immutable "owner" field), {@code categoryId} stays editable here -- an
 * expense has no owning entity of its own, category is just a mutable
 * classification, not an identity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUpdateDTO {

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
