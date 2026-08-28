package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.module.student.dto.StudentContractCreateDTO;
import com.hopestar.hfms.module.student.dto.StudentContractResponseDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.student.entity.StudentContract}.
 * A student may have multiple contracts over time, per the approved
 * business rules. {@code finalAmount} is always computed here as
 * {@code totalContractAmount - discountAmount}; callers never supply it
 * directly.
 * <p>
 * {@code remainingBalance} (exposed on {@code StudentContractResponseDTO})
 * is computed via {@link #getRemainingBalanceUsd}, which subtracts the
 * sum of the contract's {@code POSTED} {@code student_payments} (Phase 3B)
 * from {@code usdEquivalentAmount} — the single place that calculation
 * lives; the Student Payments module's overpayment check reuses this same
 * method rather than re-deriving the figure.
 */
public interface StudentContractService {

    StudentContractResponseDTO create(Long studentId, StudentContractCreateDTO createDTO);

    StudentContractResponseDTO getById(Long contractId);

    List<StudentContractResponseDTO> listByStudent(Long studentId);

    StudentContractResponseDTO cancel(Long contractId);

    StudentContractResponseDTO complete(Long contractId);

    /**
     * Computes a contract's current remaining balance in USD:
     * {@code usdEquivalentAmount - sum(posted student_payments' usdEquivalentAmount)}.
     * This is the single place that calculation lives — {@code
     * StudentPaymentServiceImpl}'s overpayment check calls this same
     * method rather than re-deriving the figure, per the approved
     * business rule that only {@code POSTED} payments affect balance.
     */
    BigDecimal getRemainingBalanceUsd(Long contractId);
}
