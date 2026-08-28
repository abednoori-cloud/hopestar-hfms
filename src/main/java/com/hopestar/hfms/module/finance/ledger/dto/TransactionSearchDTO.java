package com.hopestar.hfms.module.finance.ledger.dto;

import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Optional multi-criteria filter for {@code TransactionService.search}.
 * Every field is optional; {@code TransactionServiceImpl} composes only
 * the criteria actually supplied into a JPA {@code Specification} (see
 * {@code TransactionSpecifications}) — the same pattern used by
 * {@code StudentSearchDTO}/{@code StudentSpecifications} in the Student
 * module.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransactionSearchDTO {

    private TransactionType transactionType;

    private Direction direction;

    private TransactionStatus status;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String referenceTable;

    private Long referenceId;

    private Long branchId;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;
}
