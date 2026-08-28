package com.hopestar.hfms.module.dashboard.dto;

import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row in the Dashboard's recent-activity widget. Carries both the
 * original amount/currency and the stored USD equivalent, per the
 * project's multi-currency display convention -- never a recalculated
 * figure.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionDTO {

    private String transactionCode;
    private LocalDate transactionDate;
    private TransactionType transactionType;
    private Direction direction;
    private BigDecimal originalAmount;
    private String currencyCode;
    private BigDecimal usdEquivalentAmount;
    private TransactionStatus status;
    private String notes;
}
