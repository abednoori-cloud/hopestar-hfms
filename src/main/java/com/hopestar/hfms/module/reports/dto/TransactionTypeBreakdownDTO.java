package com.hopestar.hfms.module.reports.dto;

import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One row of the Money In/Out report's breakdown-by-type table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTypeBreakdownDTO {

    private TransactionType transactionType;
    private long count;
    private BigDecimal usdTotal;
}
