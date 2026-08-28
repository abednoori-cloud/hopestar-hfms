package com.hopestar.hfms.module.finance.salary.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Input for editing a {@code DRAFT} salary. Per the approved business
 * rules, a salary is immutable once {@code POSTED}; {@code
 * SalaryServiceImpl.update} rejects any attempt to edit a salary that is
 * not currently {@code DRAFT}. The employee/month/year a draft belongs to
 * are not editable here — cancel/void the draft and create a new one if
 * that needs to change (mirrors {@code StudentPaymentUpdateDTO}'s
 * precedent).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryUpdateDTO {

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
