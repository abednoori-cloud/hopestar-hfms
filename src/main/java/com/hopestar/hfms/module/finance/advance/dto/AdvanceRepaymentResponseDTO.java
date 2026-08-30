package com.hopestar.hfms.module.finance.advance.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for a single advance repayment, per the same "one response
 * shape, no separate List/View DTO" precedent used across the Finance
 * module. Mirrors {@code LoanRepaymentResponseDTO} exactly.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceRepaymentResponseDTO {

    private Long advanceId;
    private String advanceNumber;

    private LocalDate repaymentDate;
    private BigDecimal amount;
    private SupportedCurrency currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;

    private PaymentMethodResponseDTO paymentMethod;

    private Long ledgerTransactionId;
    private String ledgerTransactionCode;

    private String notes;
}
