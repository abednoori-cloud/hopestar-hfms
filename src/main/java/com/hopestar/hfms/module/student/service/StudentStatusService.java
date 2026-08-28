package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.module.student.dto.StudentStatusResponseDTO;

import java.util.List;

/**
 * Read-only access to the {@code student_statuses} lookup table for the
 * Student module's dropdowns. See {@link ProgramService}'s Javadoc for
 * why this stays intentionally thin in this phase.
 */
public interface StudentStatusService {

    List<StudentStatusResponseDTO> listActive();

    StudentStatusResponseDTO getById(Long id);
}
