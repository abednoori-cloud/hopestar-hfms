package com.hopestar.hfms.module.dashboard.service;

import com.hopestar.hfms.module.dashboard.dto.DashboardSummaryDTO;

/**
 * Module 2 (Dashboard): read-only aggregation over the existing Ledger,
 * Student Payment, Salary, Student, and Employee data, per the approved
 * architecture's "report and dashboard modules are read-only consumers of
 * other modules' services/repositories -- they never write" rule. Never
 * posts to the ledger, never mutates any entity.
 */
public interface DashboardService {

    /**
     * Assembles the full Dashboard view-model in one call: financial
     * summary, outstanding student receivables, salary overview, recent
     * ledger activity, and key student/employee statistics.
     */
    DashboardSummaryDTO getDashboardSummary();
}
