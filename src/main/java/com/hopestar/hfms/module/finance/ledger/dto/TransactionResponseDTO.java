package com.hopestar.hfms.module.finance.ledger.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for a ledger transaction. {@code usdEquivalentAmount} is
 * what Reports and the Dashboard (later phases) sum; {@code
 * originalAmount}/{@code currency} remain available here for
 * original-currency display, per the project's currency rule.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO extends BaseAuditDTO {

    private String transactionCode;
    private TransactionType transactionType;
    private Direction direction;
    private BigDecimal originalAmount;
    private CurrencyResponseDTO currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;
    private PaymentMethodResponseDTO paymentMethod;
    private LocalDate transactionDate;
    private String referenceTable;
    private Long referenceId;
    private TransactionStatus status;
    private String notes;
    private String branchName;
}
