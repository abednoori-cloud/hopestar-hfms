package com.hopestar.hfms.module.finance.advance.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for an employee advance. Serves both the list page and the
 * detail/view page -- one response shape, per the same precedent {@code
 * EmployeeLoanResponseDTO} set elsewhere, rather than separate "List" and
 * "View" DTOs with identical fields.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAdvanceResponseDTO extends BaseAuditDTO {

    private String advanceNumber;

    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;

    private BigDecimal advanceAmount;
    private SupportedCurrency currency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;

    private BigDecimal remainingBalance;

    private LocalDate advanceDate;

    private AdvanceStatus status;
    private String remarks;

    private Long ledgerTransactionId;
    private String ledgerTransactionCode;
}
