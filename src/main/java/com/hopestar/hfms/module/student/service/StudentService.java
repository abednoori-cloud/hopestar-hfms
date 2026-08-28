package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.student.dto.StudentCreateDTO;
import com.hopestar.hfms.module.student.dto.StudentResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentSearchDTO;
import com.hopestar.hfms.module.student.dto.StudentUpdateDTO;

/**
 * Business operations for {@code Student}, per SRS Module 3 and the
 * approved Business Rules (§4):
 * <ul>
 *   <li>student codes are always system-generated ({@code STU-2026-000001});</li>
 *   <li>passport number must be unique when provided;</li>
 *   <li>email must be unique when provided;</li>
 *   <li>students are never hard-deleted — only deactivated.</li>
 * </ul>
 */
public interface StudentService {

    StudentResponseDTO create(StudentCreateDTO createDTO);

    StudentResponseDTO update(Long id, StudentUpdateDTO updateDTO);

    StudentResponseDTO getById(Long id);

    StudentResponseDTO getByStudentCode(String studentCode);

    PageResponse<StudentResponseDTO> search(StudentSearchDTO searchDTO);

    /**
     * Soft-deletes (deactivates) a student. Per the approved business
     * rules, students are never permanently deleted — this flips
     * {@code is_active} to {@code false} and stamps {@code deleted_at}.
     */
    void deactivate(Long id);

    /** Reactivates a previously deactivated student. */
    void reactivate(Long id);
}
