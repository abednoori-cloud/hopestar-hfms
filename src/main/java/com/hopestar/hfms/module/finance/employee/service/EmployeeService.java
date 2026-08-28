package com.hopestar.hfms.module.finance.employee.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeCreateDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeResponseDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeSearchDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeUpdateDTO;

/**
 * Business operations for {@link com.hopestar.hfms.module.finance.employee.entity.Employee},
 * per the approved Phase 3C business rules:
 * <ul>
 *   <li>employee codes are always system-generated ({@code EMP-2026-000001}),
 *       via {@code SequenceGeneratorService} — the same mechanism used for
 *       student codes, invoice numbers, and transaction codes;</li>
 *   <li>phone, email, and national ID must each be unique when provided;</li>
 *   <li>USD forces {@code exchangeRateToUsd = 1.0000}; AFN/EUR require a
 *       positive, manually-entered rate — enforced via the single shared
 *       {@code MoneyUtil.resolveExchangeRateToUsd}, never duplicated here;</li>
 *   <li>{@code usdEquivalentSalary} is always computed by this service,
 *       never trusted from a caller;</li>
 *   <li>employees are never hard-deleted — only deactivated.</li>
 * </ul>
 */
public interface EmployeeService {

    EmployeeResponseDTO create(EmployeeCreateDTO createDTO);

    EmployeeResponseDTO update(Long id, EmployeeUpdateDTO updateDTO);

    EmployeeResponseDTO getById(Long id);

    EmployeeResponseDTO getByEmployeeCode(String employeeCode);

    PageResponse<EmployeeResponseDTO> search(EmployeeSearchDTO searchDTO);

    /** Soft-deletes (deactivates) an employee. Never permanently deleted, per the approved business rules. */
    void deactivate(Long id);

    /** Reactivates a previously deactivated employee. */
    void reactivate(Long id);
}
