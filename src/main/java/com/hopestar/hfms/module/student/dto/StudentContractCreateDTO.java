package com.hopestar.hfms.module.student.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input for creating a new contract for an existing student.
 * {@code finalAmount} is deliberately absent — it is always computed by
 * {@code StudentContractService} as {@code totalContractAmount -
 * discountAmount}, never supplied by the caller. Likewise
 * {@code usdEquivalentAmount} is absent: it is always computed as
 * {@code finalAmount * exchangeRateToUsd}.
 * <p>
 * {@code exchangeRateToUsd} is entered manually by the user (base
 * currency is USD; this system has no live FX feed) and is required for
 * every currency, including USD itself, where it must simply be
 * {@code 1}. The Thymeleaf form defaults it to {@code 1} and disables the
 * field when USD is selected.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentContractCreateDTO {

    @NotNull(message = "Program is required")
    private Long programId;

    @NotNull(message = "Total contract amount is required")
    @DecimalMin(value = "0.0", message = "Total contract amount cannot be negative")
    private BigDecimal totalContractAmount;

    @DecimalMin(value = "0.0", message = "Registration fee cannot be negative")
    private BigDecimal registrationFee = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Discount amount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 255)
    private String discountReason;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currencyCode;

    @NotNull(message = "Exchange rate to USD is required")
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    @NotNull(message = "Contract date is required")
    @PastOrPresent(message = "Contract date cannot be in the future")
    private LocalDate contractDate;

    @Size(max = 2000)
    private String notes;
}
