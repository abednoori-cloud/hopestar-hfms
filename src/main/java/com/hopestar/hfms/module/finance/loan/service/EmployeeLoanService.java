package com.hopestar.hfms.module.finance.loan.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanCreateDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanResponseDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanSearchDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanUpdateDTO;
import com.hopestar.hfms.module.finance.loan.dto.LoanRepaymentCreateDTO;
import com.hopestar.hfms.module.finance.loan.dto.LoanRepaymentResponseDTO;

import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.finance.loan.entity.EmployeeLoan},
 * per the approved Phase 3E business rules:
 * <ul>
 *   <li>USD forces {@code exchangeRateToUsd = 1.000000}; AFN/EUR require a
 *       positive, manually-entered rate -- enforced via the single shared
 *       {@code MoneyUtil.resolveExchangeRateToUsd}, never duplicated here;</li>
 *   <li>{@code usdEquivalentAmount} is always computed by this service,
 *       never entered manually or trusted from a caller;</li>
 *   <li>a loan is editable only while {@code DRAFT}; disbursing it posts
 *       a {@code LOAN_DISBURSEMENT} expense transaction via {@code
 *       LedgerService.postExpense} and transitions it to {@code ACTIVE}
 *       -- this service never constructs a {@code Transaction} itself;</li>
 *   <li>each repayment against an {@code ACTIVE} loan posts its own
 *       {@code LOAN_REPAYMENT} income transaction via {@code
 *       LedgerService.postIncome}, decrements {@code remainingBalance},
 *       and automatically closes the loan once the balance reaches zero;</li>
 *   <li>voiding a loan never mutates a {@code Transaction} directly --
 *       it always goes through {@code LedgerService.voidTransaction}.</li>
 * </ul>
 */
public interface EmployeeLoanService {

    /** Creates a new DRAFT loan record. */
    EmployeeLoanResponseDTO create(EmployeeLoanCreateDTO createDTO);

    /** Edits a {@code DRAFT} loan. Rejects any loan that is not currently {@code DRAFT}. */
    EmployeeLoanResponseDTO update(Long loanId, EmployeeLoanUpdateDTO updateDTO);

    /**
     * Disburses a {@code DRAFT} loan: posts a {@code LOAN_DISBURSEMENT}
     * expense transaction via {@code LedgerService.postExpense}, links it
     * to this loan, and transitions status to {@code ACTIVE}.
     */
    EmployeeLoanResponseDTO disburse(Long loanId, Long paymentMethodId);

    /**
     * Voids a loan. A {@code DRAFT} loan is simply marked {@code VOID}.
     * An {@code ACTIVE} loan additionally voids its linked ledger
     * transaction via {@code LedgerService.voidTransaction}. A {@code
     * CLOSED} (fully repaid) loan can never be voided.
     */
    EmployeeLoanResponseDTO voidLoan(Long loanId, String reason);

    /**
     * Records a repayment against an {@code ACTIVE} loan: posts a {@code
     * LOAN_REPAYMENT} income transaction via {@code LedgerService.postIncome},
     * decrements {@code remainingBalance}, and automatically transitions
     * the loan to {@code CLOSED} once the balance reaches zero.
     */
    LoanRepaymentResponseDTO recordRepayment(Long loanId, LoanRepaymentCreateDTO createDTO);

    EmployeeLoanResponseDTO getById(Long loanId);

    EmployeeLoanResponseDTO getByLoanNumber(String loanNumber);

    PageResponse<EmployeeLoanResponseDTO> search(EmployeeLoanSearchDTO searchDTO);

    /** Full loan history for an employee, most recent first. */
    List<EmployeeLoanResponseDTO> listByEmployee(Long employeeId);

    /** Full repayment history for a loan, most recent first. */
    List<LoanRepaymentResponseDTO> listRepayments(Long loanId);
}
