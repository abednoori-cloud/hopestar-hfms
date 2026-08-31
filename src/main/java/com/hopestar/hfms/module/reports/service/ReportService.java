package com.hopestar.hfms.module.reports.service;

import com.hopestar.hfms.module.reports.dto.CashFlowReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.EmployeeReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.StudentReportResponseDTO;

import java.time.LocalDate;

/**
 * Read-only reporting over the existing Ledger, Student Payment, Student
 * Contract, Salary, Loan, and Advance data -- same "report/dashboard
 * modules are read-only consumers of other modules' services/repositories,
 * they never write" rule the Dashboard module follows. Every figure here
 * reuses an existing repository aggregate query wherever one already
 * exists (several were originally built for the Dashboard) rather than
 * re-deriving a calculation that already lives somewhere else.
 */
public interface ReportService {

    /** Money In/Out report: ledger income vs. expense for a date range, with a per-type breakdown. */
    CashFlowReportResponseDTO getCashFlowReport(LocalDate from, LocalDate to);

    /**
     * Student Report: revenue collected within {@code from}/{@code to},
     * plus every currently-outstanding ACTIVE contract (a current-state
     * figure, never date-filtered -- see {@link StudentReportResponseDTO}'s
     * Javadoc). {@code programId} is optional (null means every program).
     */
    StudentReportResponseDTO getStudentReport(LocalDate from, LocalDate to, Long programId);

    /**
     * Employee Report: total payroll cost within {@code from}/{@code to},
     * plus every employee with a currently ACTIVE loan and/or advance
     * balance (current-state, not date-filtered).
     */
    EmployeeReportResponseDTO getEmployeeReport(LocalDate from, LocalDate to);
}
