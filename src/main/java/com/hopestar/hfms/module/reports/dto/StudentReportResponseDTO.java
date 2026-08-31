package com.hopestar.hfms.module.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Student Report: two deliberately separate figures that must never be
 * confused with each other -- {@link #totalRevenueUsd} is a period figure
 * (POSTED payments within {@link #from}/{@link #to}); {@link
 * #outstandingStudents}/{@link #totalOutstandingUsd} are a current-state
 * figure (every ACTIVE contract with a positive remaining balance right
 * now, regardless of the selected date range) -- see {@code
 * ReportServiceImpl#getStudentReport} for why.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReportResponseDTO {

    private LocalDate from;
    private LocalDate to;

    private Long programId;
    /** "All Programs" when {@link #programId} is {@code null}. */
    private String programName;

    private BigDecimal totalRevenueUsd;

    private BigDecimal totalOutstandingUsd;
    private long studentsOwingCount;
    private List<OutstandingStudentDTO> outstandingStudents;
}
