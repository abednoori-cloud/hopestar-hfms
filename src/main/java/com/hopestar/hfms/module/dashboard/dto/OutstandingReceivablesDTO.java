package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard's outstanding student receivables widget. Scoped to {@code
 * ACTIVE} contracts only -- see the Module 2 final report for why
 * cancelled/completed contracts are excluded from this figure.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingReceivablesDTO {

    private BigDecimal totalOutstandingUsd;
    private long studentsWithOutstandingBalanceCount;
    private List<OutstandingContractDTO> topOutstandingContracts;
}
