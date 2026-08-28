package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One active contract with a positive remaining balance, for the
 * Dashboard's outstanding-receivables widget. {@link #remainingBalanceUsd}
 * is computed with the exact same formula as {@code
 * StudentContractServiceImpl#getRemainingBalanceUsd} (contract's USD
 * equivalent minus its posted payments) -- reusing the same underlying
 * repository sum, not a second, parallel calculation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingContractDTO {

    private Long contractId;
    private Long studentId;
    private String studentCode;
    private String studentFullName;
    private BigDecimal contractUsdEquivalentAmount;
    private BigDecimal remainingBalanceUsd;
}
