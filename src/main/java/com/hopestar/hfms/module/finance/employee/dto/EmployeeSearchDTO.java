package com.hopestar.hfms.module.finance.employee.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional multi-criteria filter for the employee list/search page.
 * Every field is optional; {@code EmployeeServiceImpl} composes only the
 * criteria actually supplied into a JPA {@code Specification} (see
 * {@code EmployeeSpecifications}), mirroring {@code StudentSearchDTO}/
 * {@code StudentSpecifications} in the Student module exactly.
 */
@Getter
@Setter
@NoArgsConstructor
public class EmployeeSearchDTO {

    /** Matches against full name (contains, case-insensitive). */
    private String name;

    /** Matches against employee code (contains, case-insensitive). */
    private String employeeCode;

    /** Matches against phone (contains). */
    private String phone;

    /** Matches against email (contains, case-insensitive). */
    private String email;

    /** Matches against department (contains, case-insensitive). */
    private String department;

    /** Matches against position (contains, case-insensitive). */
    private String position;

    private Long employmentStatusId;

    private Long branchId;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;

    /** Sort field: one of fullName, employeeCode, joiningDate, baseSalary. Defaults to fullName. */
    private String sortBy = "fullName";

    /** Sort direction: ASC or DESC. Defaults to ASC. */
    private String sortDirection = "ASC";
}
