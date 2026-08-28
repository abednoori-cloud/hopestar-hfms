package com.hopestar.hfms.module.finance.studentpayment.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentCreateDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentResponseDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentSearchDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentUpdateDTO;

import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.finance.studentpayment.entity.StudentPayment},
 * per the approved Phase 3B business rules:
 * <ul>
 *   <li>student and contract must exist and the contract must be {@code ACTIVE};</li>
 *   <li>payment amount must be positive;</li>
 *   <li>USD forces {@code exchangeRateToUsd = 1.0000}; AFN/EUR require a
 *       positive, manually-entered rate — enforced here, never trusted
 *       from a caller;</li>
 *   <li>a payment can never exceed the contract's current remaining
 *       balance (see {@code StudentContractService.getRemainingBalanceUsd},
 *       the single place that figure is computed);</li>
 *   <li>only {@code POSTED} payments affect that balance;</li>
 *   <li>every {@code POSTED} payment has a corresponding ledger entry,
 *       created exclusively via {@code LedgerService.postIncome} — this
 *       service never constructs a {@code Transaction} itself;</li>
 *   <li>a payment is immutable once {@code POSTED} — only a {@code DRAFT}
 *       payment can be edited.</li>
 * </ul>
 */
public interface StudentPaymentService {

    /**
     * Records a payment. If {@code createDTO.saveAsDraft} is {@code
     * false} (the default), the payment is fully validated — including
     * the overpayment check — and posted to the ledger immediately. If
     * {@code true}, it is saved as {@code DRAFT} (still validated for a
     * positive amount and valid currency/rate) without touching the
     * ledger or checking against the balance yet; call {@link
     * #post(Long)} later to finalize it.
     */
    StudentPaymentResponseDTO create(StudentPaymentCreateDTO createDTO);

    /** Edits a {@code DRAFT} payment. Rejects any payment that is not currently {@code DRAFT}. */
    StudentPaymentResponseDTO update(Long paymentId, StudentPaymentUpdateDTO updateDTO);

    /**
     * Finalizes a {@code DRAFT} payment: re-validates the contract is
     * still {@code ACTIVE}, re-checks the overpayment rule against the
     * contract's current remaining balance, posts the transaction via
     * {@code LedgerService.postIncome}, links it to this payment, and
     * transitions status to {@code POSTED}.
     */
    StudentPaymentResponseDTO post(Long paymentId);

    /**
     * Cancels a payment. A {@code DRAFT} payment is simply marked {@code
     * CANCELLED}. A {@code POSTED} payment additionally voids its linked
     * ledger transaction via {@code LedgerService.voidTransaction} —
     * this service never mutates a {@code Transaction} directly — which
     * removes it from the contract's posted-payment total.
     */
    StudentPaymentResponseDTO cancel(Long paymentId, String reason);

    StudentPaymentResponseDTO getById(Long paymentId);

    StudentPaymentResponseDTO getByPaymentNumber(String paymentNumber);

    PageResponse<StudentPaymentResponseDTO> search(StudentPaymentSearchDTO searchDTO);

    /** Full payment history for a student, most recent first. */
    List<StudentPaymentResponseDTO> listByStudent(Long studentId);

    /** All payments recorded against a single contract, most recent first. */
    List<StudentPaymentResponseDTO> listByContract(Long contractId);
}
