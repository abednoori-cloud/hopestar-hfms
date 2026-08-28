package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.module.student.dto.ProgramResponseDTO;

import java.util.List;

/**
 * Read-only access to the {@code programs} master list for the Student
 * module's dropdowns and lookups. Full Program CRUD (creating/editing
 * price-list entries) is out of scope for the Student Management module
 * as specified and is expected to land alongside a future Settings/
 * Catalog screen; this service exposes exactly what Student Management
 * needs today.
 */
public interface ProgramService {

    List<ProgramResponseDTO> listActive();

    ProgramResponseDTO getById(Long id);
}
