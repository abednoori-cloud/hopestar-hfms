package com.hopestar.hfms.module.finance.studentpayment.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.module.finance.ledger.dto.CurrencyResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for a student payment. {@code contractRemainingBalance} is
 * the linked contract's current remaining balance (in USD), fetched from
 * {@code StudentContractService.getRemainingBalanceUsd} — the single
 * place that calculation lives — not recomputed here.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPaymentResponseDTO extends BaseAuditDTO {

    private String paymentNumber;
    private String receiptNumber;

    private Long studentId;
    private String studentCode;
    private String studentFullName;

    private Long contractId;
    private BigDecimal contractRemainingBalance;

    private Long transactionId;
    private String transactionCode;

    private LocalDate paymentDate;
    private BigDecimal originalAmount;
    private CurrencyResponseDTO currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;
    private PaymentMethodResponseDTO paymentMethod;
    private String referenceNumber;
    private String notes;
    private PaymentStatus status;
}
