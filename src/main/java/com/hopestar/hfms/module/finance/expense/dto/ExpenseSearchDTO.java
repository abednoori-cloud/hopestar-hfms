package com.hopestar.hfms.module.finance.expense.dto;

import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Optional multi-criteria filter for the expense list/search page. Every
 * field is optional; {@code ExpenseServiceImpl} composes only the
 * criteria actually supplied into a JPA {@code Specification} (see
 * {@code ExpenseSpecifications}), mirroring {@code EmployeeAdvanceSearchDTO}
 * exactly.
 */
@Getter
@Setter
@NoArgsConstructor
public class ExpenseSearchDTO {

    private Long categoryId;

    private ExpenseStatus status;

    /** Matches against the expense description (contains, case-insensitive). */
    private String description;

    /** Inclusive lower bound on {@code expenseDate}. */
    private LocalDate dateFrom;

    /** Inclusive upper bound on {@code expenseDate}. */
    private LocalDate dateTo;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;

    /** Sort field: one of expenseNumber, expenseDate, usdEquivalentAmount. Defaults to expenseDate. */
    private String sortBy = "expenseDate";

    /** Sort direction: ASC or DESC. Defaults to DESC (most recent first). */
    private String sortDirection = "DESC";
}
