package com.hopestar.hfms.module.finance.salary.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.salary.dto.SalaryCreateDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalaryResponseDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalarySearchDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalaryUpdateDTO;

import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.finance.salary.entity.Salary},
 * per the approved Phase 3D business rules:
 * <ul>
 *   <li>exactly one salary record per employee per month/year;</li>
 *   <li>USD forces {@code exchangeRateToUsd = 1.0000}; AFN/EUR require a
 *       positive, manually-entered rate — enforced via the single shared
 *       {@code MoneyUtil.resolveExchangeRateToUsd}, never duplicated here;</li>
 *   <li>{@code usdEquivalentSalary} (of the basic salary) and {@code
 *       netSalary} are always computed by this service, never entered
 *       manually or trusted from a caller;</li>
 *   <li>a salary is editable only while {@code DRAFT}; it becomes
 *       read-only once {@code POSTED};</li>
 *   <li>posting a salary automatically creates an {@code EXPENSE}
 *       transaction via {@code LedgerService.postExpense} — this service
 *       never constructs a {@code Transaction} itself.</li>
 * </ul>
 */
public interface SalaryService {

    /** Creates a new DRAFT salary record. Fails if one already exists for the same employee/month/year. */
    SalaryResponseDTO create(SalaryCreateDTO createDTO);

    /** Edits a {@code DRAFT} salary. Rejects any salary that is not currently {@code DRAFT}. */
    SalaryResponseDTO update(Long salaryId, SalaryUpdateDTO updateDTO);

    /**
     * Posts a {@code DRAFT} salary: recomputes and finalizes {@code
     * netSalary}, posts an {@code EXPENSE} transaction via {@code
     * LedgerService.postExpense}, links it to this salary, stamps {@code
     * paymentDate} to today, and transitions status to {@code POSTED}.
     */
    SalaryResponseDTO post(Long salaryId);

    /**
     * Voids a salary. A {@code DRAFT} salary is simply marked {@code
     * VOID}. A {@code POSTED} salary additionally voids its linked
     * ledger transaction via {@code LedgerService.voidTransaction} —
     * this service never mutates a {@code Transaction} directly.
     */
    SalaryResponseDTO voidSalary(Long salaryId, String reason);

    SalaryResponseDTO getById(Long salaryId);

    SalaryResponseDTO getBySalaryNumber(String salaryNumber);

    PageResponse<SalaryResponseDTO> search(SalarySearchDTO searchDTO);

    /** Full salary history for an employee, most recent first. */
    List<SalaryResponseDTO> listByEmployee(Long employeeId);
}
