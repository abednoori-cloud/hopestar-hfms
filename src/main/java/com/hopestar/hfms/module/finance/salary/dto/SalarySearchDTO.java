package com.hopestar.hfms.module.finance.salary.dto;

import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional multi-criteria filter for the salary list/search page. Every
 * field is optional; {@code SalaryServiceImpl} composes only the
 * criteria actually supplied into a JPA {@code Specification} (see
 * {@code SalarySpecifications}), mirroring {@code StudentSearchDTO}/
 * {@code StudentSpecifications} and {@code EmployeeSearchDTO}/{@code
 * EmployeeSpecifications} exactly.
 */
@Getter
@Setter
@NoArgsConstructor
public class SalarySearchDTO {

    /** Matches against the linked employee's full name (contains, case-insensitive). */
    private String employeeName;

    /** Matches against the linked employee's code (contains, case-insensitive). */
    private String employeeCode;

    private Long employeeId;

    private Integer month;

    private Integer year;

    private SalaryPaymentStatus paymentStatus;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;

    /** Sort field: one of salaryNumber, year, month, netSalary. Defaults to year. */
    private String sortBy = "year";

    /** Sort direction: ASC or DESC. Defaults to DESC (most recent first). */
    private String sortDirection = "DESC";
}
