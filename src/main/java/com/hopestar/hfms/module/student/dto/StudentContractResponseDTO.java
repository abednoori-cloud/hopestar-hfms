package com.hopestar.hfms.module.student.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for a contract. {@code remainingBalance} is computed by
 * {@code StudentContractService} at read time (see that interface's
 * Javadoc for how this will evolve once the Finance module's
 * {@code student_payments} ledger exists) rather than being a stored
 * column, so it can never drift out of sync with payment history.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentContractResponseDTO extends BaseAuditDTO {

    private Long studentId;
    private String studentCode;
    private String studentFullName;
    private ProgramResponseDTO program;
    private BigDecimal totalContractAmount;
    private BigDecimal registrationFee;
    private BigDecimal discountAmount;
    private String discountReason;
    private BigDecimal finalAmount;
    private BigDecimal remainingBalance;
    private SupportedCurrency currencyCode;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentAmount;
    private LocalDate contractDate;
    private ContractStatus status;
    private String notes;
}
