package com.hopestar.hfms.module.finance.advance.dto;

import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional multi-criteria filter for the employee-advance list/search
 * page. Every field is optional; {@code EmployeeAdvanceServiceImpl}
 * composes only the criteria actually supplied into a JPA {@code
 * Specification} (see {@code EmployeeAdvanceSpecifications}), mirroring
 * {@code EmployeeLoanSearchDTO} exactly.
 */
@Getter
@Setter
@NoArgsConstructor
public class EmployeeAdvanceSearchDTO {

    /** Matches against the linked employee's full name (contains, case-insensitive). */
    private String employeeName;

    /** Matches against the linked employee's code (contains, case-insensitive). */
    private String employeeCode;

    private Long employeeId;

    private AdvanceStatus status;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;

    /** Sort field: one of advanceNumber, advanceDate, remainingBalance. Defaults to advanceDate. */
    private String sortBy = "advanceDate";

    /** Sort direction: ASC or DESC. Defaults to DESC (most recent first). */
    private String sortDirection = "DESC";
}
