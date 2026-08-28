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
 * Input for recording a new student payment. {@code usdEquivalentAmount},
 * {@code paymentNumber}, and {@code receiptNumber} are deliberately
 * absent — {@code StudentPaymentServiceImpl} always computes/generates
 * them, never trusting a caller for the USD conversion (per the
 * project's currency rule) or for a business-key it must generate
 * itself.
 * <p>
 * {@code saveAsDraft}, when {@code true}, saves the payment without
 * posting it to the ledger (no overpayment check beyond a positive
 * amount, no {@code Transaction} row yet) — it can later be finalized via
 * {@code StudentPaymentService.post(id)}. When {@code false} (the
 * default), the payment is validated in full — including the overpayment
 * check against the contract's current remaining balance — and posted to
 * the ledger immediately via {@code LedgerService.postIncome}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentPaymentCreateDTO {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Contract is required")
    private Long contractId;

    private Long invoiceId;

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

    private boolean saveAsDraft = false;
}
