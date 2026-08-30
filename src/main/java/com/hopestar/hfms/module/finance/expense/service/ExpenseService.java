package com.hopestar.hfms.module.finance.expense.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseCreateDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseResponseDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseSearchDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseUpdateDTO;

import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.finance.expense.entity.Expense},
 * a simple office cost with no repayment/schedule concept, mirroring
 * {@code EmployeeAdvanceService}'s business rules where they still apply:
 * <ul>
 *   <li>USD forces {@code exchangeRateToUsd = 1.000000}; AFN/EUR require a
 *       positive, manually-entered rate -- enforced via the single shared
 *       {@code MoneyUtil.resolveExchangeRateToUsd}, never duplicated here;</li>
 *   <li>{@code usdEquivalentAmount} is always computed by this service via
 *       {@code MoneyUtil.multiply}, never entered manually or trusted
 *       from a caller;</li>
 *   <li>an expense is editable only while {@code DRAFT}; posting it posts
 *       an {@code EXPENSE} transaction via {@code LedgerService.postExpense}
 *       and transitions it straight to {@code POSTED} -- there is no
 *       separate approval step and no {@code ACTIVE} state, since an
 *       expense has no ongoing balance to track once posted; this service
 *       never constructs a {@code Transaction} itself;</li>
 *   <li>voiding an expense never mutates a {@code Transaction} directly
 *       -- it always goes through {@code LedgerService.voidTransaction}.</li>
 * </ul>
 */
public interface ExpenseService {

    /** Creates a new DRAFT expense record. */
    ExpenseResponseDTO create(ExpenseCreateDTO createDTO);

    /** Edits a {@code DRAFT} expense. Rejects any expense that is not currently {@code DRAFT}. */
    ExpenseResponseDTO update(Long expenseId, ExpenseUpdateDTO updateDTO);

    /**
     * Posts a {@code DRAFT} expense: posts an {@code EXPENSE} transaction
     * via {@code LedgerService.postExpense}, links it to this expense,
     * and transitions status straight to {@code POSTED} -- posting is the
     * final step, there is no separate approval action.
     */
    ExpenseResponseDTO post(Long expenseId, Long paymentMethodId);

    /**
     * Voids an expense. A {@code DRAFT} expense is simply marked {@code
     * VOID} (nothing has posted to the ledger yet). A {@code POSTED}
     * expense additionally voids its linked ledger transaction via
     * {@code LedgerService.voidTransaction}. An expense that is already
     * {@code VOID} can never be voided again.
     */
    ExpenseResponseDTO voidExpense(Long expenseId, String reason);

    ExpenseResponseDTO getById(Long expenseId);

    ExpenseResponseDTO getByExpenseNumber(String expenseNumber);

    PageResponse<ExpenseResponseDTO> search(ExpenseSearchDTO searchDTO);

    /** Every active expense, most recent first. */
    List<ExpenseResponseDTO> listAll();
}
