package com.hopestar.hfms.module.finance.advance.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.advance.dto.AdvanceRepaymentCreateDTO;
import com.hopestar.hfms.module.finance.advance.dto.AdvanceRepaymentResponseDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceCreateDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceResponseDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceSearchDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceUpdateDTO;

import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.finance.advance.entity.EmployeeAdvance},
 * mirroring {@code EmployeeLoanService}'s business rules exactly:
 * <ul>
 *   <li>USD forces {@code exchangeRateToUsd = 1.000000}; AFN/EUR require a
 *       positive, manually-entered rate -- enforced via the single shared
 *       {@code MoneyUtil.resolveExchangeRateToUsd}, never duplicated here;</li>
 *   <li>{@code usdEquivalentAmount} is always computed by this service via
 *       {@code MoneyUtil.multiply}, never entered manually or trusted from
 *       a caller;</li>
 *   <li>an advance is editable only while {@code DRAFT}; disbursing it
 *       posts an {@code ADVANCE} expense transaction via {@code
 *       LedgerService.postExpense} and transitions it to {@code ACTIVE}
 *       -- this service never constructs a {@code Transaction} itself;</li>
 *   <li>each repayment against an {@code ACTIVE} advance posts its own
 *       {@code ADVANCE_REPAYMENT} income transaction via {@code
 *       LedgerService.postIncome}, decrements {@code remainingBalance},
 *       and automatically closes the advance once the balance reaches
 *       zero;</li>
 *   <li>voiding an advance never mutates a {@code Transaction} directly
 *       -- it always goes through {@code LedgerService.voidTransaction}.</li>
 * </ul>
 */
public interface EmployeeAdvanceService {

    /** Creates a new DRAFT advance record. */
    EmployeeAdvanceResponseDTO create(EmployeeAdvanceCreateDTO createDTO);

    /** Edits a {@code DRAFT} advance. Rejects any advance that is not currently {@code DRAFT}. */
    EmployeeAdvanceResponseDTO update(Long advanceId, EmployeeAdvanceUpdateDTO updateDTO);

    /**
     * Disburses a {@code DRAFT} advance: posts an {@code ADVANCE} expense
     * transaction via {@code LedgerService.postExpense}, links it to this
     * advance, and transitions status to {@code ACTIVE}.
     */
    EmployeeAdvanceResponseDTO disburse(Long advanceId, Long paymentMethodId);

    /**
     * Voids an advance. A {@code DRAFT} advance is simply marked {@code
     * VOID}. An {@code ACTIVE} advance additionally voids its linked
     * ledger transaction via {@code LedgerService.voidTransaction}. A
     * {@code CLOSED} (fully repaid) advance can never be voided.
     */
    EmployeeAdvanceResponseDTO voidAdvance(Long advanceId, String reason);

    /**
     * Records a repayment against an {@code ACTIVE} advance: posts an
     * {@code ADVANCE_REPAYMENT} income transaction via {@code
     * LedgerService.postIncome}, decrements {@code remainingBalance}, and
     * automatically transitions the advance to {@code CLOSED} once the
     * balance reaches zero.
     */
    AdvanceRepaymentResponseDTO recordRepayment(Long advanceId, AdvanceRepaymentCreateDTO createDTO);

    EmployeeAdvanceResponseDTO getById(Long advanceId);

    EmployeeAdvanceResponseDTO getByAdvanceNumber(String advanceNumber);

    PageResponse<EmployeeAdvanceResponseDTO> search(EmployeeAdvanceSearchDTO searchDTO);

    /** Full advance history for an employee, most recent first. */
    List<EmployeeAdvanceResponseDTO> listByEmployee(Long employeeId);

    /** Full repayment history for an advance, most recent first. */
    List<AdvanceRepaymentResponseDTO> listRepayments(Long advanceId);
}
