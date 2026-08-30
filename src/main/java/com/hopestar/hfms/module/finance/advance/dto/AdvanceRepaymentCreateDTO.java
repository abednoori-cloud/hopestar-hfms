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
 * Input for recording a repayment against an {@code ACTIVE} employee
 * advance. {@code usdEquivalentAmount} is deliberately absent -- always
 * computed by {@code EmployeeAdvanceServiceImpl} from {@code amount} and
 * {@code exchangeRateToUsd}, per the project's currency rule. Mirrors
 * {@code LoanRepaymentCreateDTO} exactly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceRepaymentCreateDTO {

    @NotNull(message = "Repayment date is required")
    @PastOrPresent(message = "Repayment date cannot be in the future")
    private LocalDate repaymentDate;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @NotNull(message = "Payment method is required")
    private Long paymentMethodId;

    @Size(max = 2000)
    private String notes;
}
