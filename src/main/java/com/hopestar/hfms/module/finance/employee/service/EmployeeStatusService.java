package com.hopestar.hfms.module.finance.employee.service;

import com.hopestar.hfms.module.finance.employee.dto.EmployeeStatusResponseDTO;

import java.util.List;

/**
 * Read-only access to the {@code employee_statuses} lookup table for the
 * Employee module's dropdowns, mirroring {@code StudentStatusService} in
 * the Student module.
 */
public interface EmployeeStatusService {

    List<EmployeeStatusResponseDTO> listActive();

    EmployeeStatusResponseDTO getById(Long id);
}
