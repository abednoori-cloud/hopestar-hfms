package com.hopestar.hfms.module.finance.studentpayment.dto;

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
 * Input for editing a {@code DRAFT} payment. Per the approved business
 * rules, a payment is immutable once {@code POSTED} (its historical
 * exchange rate and ledger entry must never change); {@code
 * StudentPaymentServiceImpl.update} rejects any attempt to edit a payment
 * that is not currently {@code DRAFT}. The student and contract a draft
 * belongs to are not editable here — cancel the draft and create a new
 * one if that needs to change.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentPaymentUpdateDTO {

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    @NotNull(message = "Original amount is required")
    @Positive(message = "Payment amount must be positive")
    private BigDecimal originalAmount;

    @NotNull(message = "Currency is required")
    private SupportedCurrency currencyCode;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @NotNull(message = "Payment method is required")
    private Long paymentMethodId;

    @Size(max = 100)
    private String referenceNumber;

    @Size(max = 2000)
    private String notes;
}
