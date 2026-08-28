package com.hopestar.hfms.module.finance.loan.dto;

import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional multi-criteria filter for the employee-loan list/search page.
 * Every field is optional; {@code EmployeeLoanServiceImpl} composes only
 * the criteria actually supplied into a JPA {@code Specification} (see
 * {@code EmployeeLoanSpecifications}), mirroring {@code SalarySearchDTO}/
 * {@code SalarySpecifications} exactly.
 */
@Getter
@Setter
@NoArgsConstructor
public class EmployeeLoanSearchDTO {

    /** Matches against the linked employee's full name (contains, case-insensitive). */
    private String employeeName;

    /** Matches against the linked employee's code (contains, case-insensitive). */
    private String employeeCode;

    private Long employeeId;

    private LoanStatus status;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;

    /** Sort field: one of loanNumber, loanDate, remainingBalance. Defaults to loanDate. */
    private String sortBy = "loanDate";

    /** Sort direction: ASC or DESC. Defaults to DESC (most recent first). */
    private String sortDirection = "DESC";
}
