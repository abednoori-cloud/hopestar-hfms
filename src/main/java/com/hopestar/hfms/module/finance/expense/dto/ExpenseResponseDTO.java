package com.hopestar.hfms.module.finance.expense.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for an expense. Serves both the list page and the detail/
 * view page -- one response shape, per the same precedent {@code
 * EmployeeAdvanceResponseDTO} set elsewhere, rather than separate "List"
 * and "View" DTOs with identical fields.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponseDTO extends BaseAuditDTO {

    private String expenseNumber;

    private Long categoryId;
    private String categoryName;

    private String description;

    private BigDecimal amount;
    private SupportedCurrency currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;

    private LocalDate expenseDate;

    private ExpenseStatus status;
    private String remarks;

    private Long ledgerTransactionId;
    private String ledgerTransactionCode;
}
