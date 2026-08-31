package com.hopestar.hfms.module.dashboard.service;

import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.dashboard.dto.DashboardSummaryDTO;
import com.hopestar.hfms.module.dashboard.dto.FinancialSummaryDTO;
import com.hopestar.hfms.module.dashboard.dto.LoanAdvanceExpenseSummaryDTO;
import com.hopestar.hfms.module.dashboard.dto.OutstandingContractDTO;
import com.hopestar.hfms.module.dashboard.dto.OutstandingReceivablesDTO;
import com.hopestar.hfms.module.dashboard.dto.PendingSalaryDTO;
import com.hopestar.hfms.module.dashboard.dto.RecentTransactionDTO;
import com.hopestar.hfms.module.dashboard.dto.SalaryOverviewDTO;
import com.hopestar.hfms.module.dashboard.dto.StudentEmployeeStatsDTO;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import com.hopestar.hfms.module.finance.advance.repository.EmployeeAdvanceRepository;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeRepository;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import com.hopestar.hfms.module.finance.expense.repository.ExpenseRepository;
import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import com.hopestar.hfms.module.finance.loan.repository.EmployeeLoanRepository;
import com.hopestar.hfms.module.finance.salary.entity.Salary;
import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import com.hopestar.hfms.module.finance.salary.repository.SalaryRepository;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.repository.StudentPaymentRepository;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import com.hopestar.hfms.module.student.entity.StudentContract;
import com.hopestar.hfms.module.student.repository.StudentContractRepository;
import com.hopestar.hfms.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implements {@link DashboardService}. Reads directly from existing
 * repositories (permitted for a read-only aggregation layer per the
 * approved architecture -- see the interface Javadoc) rather than routing
 * every figure through each module's full service layer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    /** Bounded candidate pool sizes -- the Dashboard never loads a whole table. */
    private static final int OUTSTANDING_CONTRACT_DISPLAY_LIMIT = 5;
    private static final int PENDING_SALARY_DISPLAY_LIMIT = 5;
    private static final int RECENT_TRANSACTION_LIMIT = 8;

    private final TransactionRepository transactionRepository;
    private final StudentContractRepository studentContractRepository;
    private final StudentPaymentRepository studentPaymentRepository;
    private final SalaryRepository salaryRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeAdvanceRepository employeeAdvanceRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        return DashboardSummaryDTO.builder()
                .financialSummary(buildFinancialSummary())
                .outstandingReceivables(buildOutstandingReceivables())
                .salaryOverview(buildSalaryOverview())
                .loanAdvanceExpenseSummary(buildLoanAdvanceExpenseSummary())
                .recentTransactions(buildRecentTransactions())
                .stats(buildStats())
                .build();
    }

    // ---------------------------------------------------------------
    // Financial summary (section 2)
    // ---------------------------------------------------------------

    private FinancialSummaryDTO buildFinancialSummary() {
        BigDecimal totalIncome = transactionRepository.sumUsdEquivalentAmountByDirectionAndStatus(
                Direction.INCOME, TransactionStatus.POSTED);
        BigDecimal totalExpense = transactionRepository.sumUsdEquivalentAmountByDirectionAndStatus(
                Direction.EXPENSE, TransactionStatus.POSTED);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate periodEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal periodIncome = transactionRepository.sumUsdEquivalentAmountByDirectionAndStatusAndDateRange(
                Direction.INCOME, TransactionStatus.POSTED, periodStart, periodEnd);
        BigDecimal periodExpense = transactionRepository.sumUsdEquivalentAmountByDirectionAndStatusAndDateRange(
                Direction.EXPENSE, TransactionStatus.POSTED, periodStart, periodEnd);

        return FinancialSummaryDTO.builder()
                .totalIncomeUsd(totalIncome)
                .totalExpenseUsd(totalExpense)
                .netPositionUsd(MoneyUtil.subtract(totalIncome, totalExpense))
                .currentPeriodIncomeUsd(periodIncome)
                .currentPeriodExpenseUsd(periodExpense)
                .currentPeriodNetUsd(MoneyUtil.subtract(periodIncome, periodExpense))
                .currentPeriodLabel(monthLabel(today))
                .build();
    }

    // ---------------------------------------------------------------
    // Outstanding student receivables (section 3)
    // ---------------------------------------------------------------

    /**
     * Total outstanding = (total USD value of ACTIVE contracts) - (total
     * USD of POSTED payments against ACTIVE contracts), two independent
     * flat sums rather than a join (a join would double-count a contract
     * with more than one posted payment). Scoped to {@code ACTIVE}
     * contracts only: CANCELLED/COMPLETED contracts are excluded from
     * "outstanding" by design, since neither the SRS nor the existing
     * model defines whether a leftover balance on a cancelled contract is
     * still a receivable -- see the Module 2 final report.
     * <p>
     * The top-N list is computed, filtered, and sorted in a single query
     * ({@code StudentContractRepository
     * .findTopOutstandingContractsWithRemainingBalance}) rather than
     * loading a candidate pool and issuing one remaining-balance query per
     * contract (a former N+1, since fixed).
     */
    private OutstandingReceivablesDTO buildOutstandingReceivables() {
        BigDecimal totalContractValue = studentContractRepository
                .sumUsdEquivalentAmountByStatusAndActiveTrue(ContractStatus.ACTIVE);
        BigDecimal totalPosted = studentPaymentRepository
                .sumUsdEquivalentAmountByStatusAndContractStatus(PaymentStatus.POSTED, ContractStatus.ACTIVE);
        BigDecimal totalOutstanding = MoneyUtil.subtract(totalContractValue, totalPosted);

        long studentsWithOutstanding = studentContractRepository.countDistinctStudentsWithOutstandingBalance(
                ContractStatus.ACTIVE, PaymentStatus.POSTED);

        List<Object[]> rows = studentContractRepository.findTopOutstandingContractsWithRemainingBalance(
                ContractStatus.ACTIVE, PaymentStatus.POSTED, null, PageRequest.of(0, OUTSTANDING_CONTRACT_DISPLAY_LIMIT));

        List<OutstandingContractDTO> topOutstanding = rows.stream()
                .map(row -> {
                    StudentContract contract = (StudentContract) row[0];
                    BigDecimal remaining = (BigDecimal) row[1];
                    return OutstandingContractDTO.builder()
                            .contractId(contract.getId())
                            .studentId(contract.getStudent().getId())
                            .studentCode(contract.getStudent().getStudentCode())
                            .studentFullName(contract.getStudent().getFullName())
                            .contractUsdEquivalentAmount(contract.getUsdEquivalentAmount())
                            .remainingBalanceUsd(remaining)
                            .build();
                })
                .toList();

        return OutstandingReceivablesDTO.builder()
                .totalOutstandingUsd(totalOutstanding)
                .studentsWithOutstandingBalanceCount(studentsWithOutstanding)
                .topOutstandingContracts(topOutstanding)
                .build();
    }

    // ---------------------------------------------------------------
    // Salary overview (section 4)
    // ---------------------------------------------------------------

    private SalaryOverviewDTO buildSalaryOverview() {
        long pendingCount = salaryRepository.countByPaymentStatusAndActiveTrue(SalaryPaymentStatus.DRAFT);
        BigDecimal pendingBasicPay = salaryRepository
                .sumUsdEquivalentSalaryByPaymentStatusAndActiveTrue(SalaryPaymentStatus.DRAFT);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int month = today.getMonthValue();
        int year = today.getYear();

        long currentPeriodDraft = salaryRepository
                .countByPaymentStatusAndMonthAndYearAndActiveTrue(SalaryPaymentStatus.DRAFT, month, year);
        long currentPeriodPosted = salaryRepository
                .countByPaymentStatusAndMonthAndYearAndActiveTrue(SalaryPaymentStatus.POSTED, month, year);

        List<Salary> pending = salaryRepository.findByPaymentStatusAndActiveTrueWithEmployeeOrderByYearDescMonthDesc(
                SalaryPaymentStatus.DRAFT, PageRequest.of(0, PENDING_SALARY_DISPLAY_LIMIT));

        List<PendingSalaryDTO> pendingDTOs = pending.stream()
                .map(salary -> PendingSalaryDTO.builder()
                        .salaryId(salary.getId())
                        .employeeId(salary.getEmployee().getId())
                        .employeeCode(salary.getEmployee().getEmployeeCode())
                        .employeeFullName(salary.getEmployee().getFullName())
                        .month(salary.getMonth())
                        .year(salary.getYear())
                        .netSalary(salary.getNetSalary())
                        .currency(salary.getCurrency())
                        .build())
                .toList();

        return SalaryOverviewDTO.builder()
                .pendingSalaryCount(pendingCount)
                .pendingBasicPayUsd(pendingBasicPay)
                .currentPeriodTotalCount(currentPeriodDraft + currentPeriodPosted)
                .currentPeriodDraftCount(currentPeriodDraft)
                .currentPeriodPostedCount(currentPeriodPosted)
                .currentPeriodLabel(monthLabel(today))
                .pendingSalaries(pendingDTOs)
                .build();
    }

    // ---------------------------------------------------------------
    // Loans / advances / expenses (section 5.5)
    // ---------------------------------------------------------------

    /**
     * Loan/advance figures are scoped to {@code ACTIVE} records only,
     * mirroring the outstanding-receivables widget's {@code ACTIVE}-only
     * scoping. The expense figure is the current calendar month's {@code
     * POSTED} total, computed with the same first-of-month/last-of-month
     * date range used by {@link #buildFinancialSummary()}.
     */
    private LoanAdvanceExpenseSummaryDTO buildLoanAdvanceExpenseSummary() {
        long activeLoanCount = employeeLoanRepository.countByStatusAndActiveTrue(LoanStatus.ACTIVE);
        BigDecimal loanOutstanding = employeeLoanRepository
                .sumRemainingBalanceUsdByStatusAndActiveTrue(LoanStatus.ACTIVE);

        long activeAdvanceCount = employeeAdvanceRepository.countByStatusAndActiveTrue(AdvanceStatus.ACTIVE);
        BigDecimal advanceOutstanding = employeeAdvanceRepository
                .sumRemainingBalanceUsdByStatusAndActiveTrue(AdvanceStatus.ACTIVE);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate periodEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        BigDecimal currentPeriodExpense = expenseRepository
                .sumUsdEquivalentAmountByStatusAndDateRange(ExpenseStatus.POSTED, periodStart, periodEnd);

        return LoanAdvanceExpenseSummaryDTO.builder()
                .activeLoanCount(activeLoanCount)
                .loanOutstandingUsd(loanOutstanding)
                .activeAdvanceCount(activeAdvanceCount)
                .advanceOutstandingUsd(advanceOutstanding)
                .currentPeriodExpenseUsd(currentPeriodExpense)
                .currentPeriodLabel(monthLabel(today))
                .build();
    }

    // ---------------------------------------------------------------
    // Recent transactions (section 6)
    // ---------------------------------------------------------------

    private List<RecentTransactionDTO> buildRecentTransactions() {
        Pageable pageable = PageRequest.of(0, RECENT_TRANSACTION_LIMIT);
        List<Transaction> recent = transactionRepository
                .findByStatusAndActiveTrueWithCurrencyOrderByDateDesc(TransactionStatus.POSTED, pageable);

        return recent.stream()
                .map(t -> RecentTransactionDTO.builder()
                        .transactionCode(t.getTransactionCode())
                        .transactionDate(t.getTransactionDate())
                        .transactionType(t.getTransactionType())
                        .direction(t.getDirection())
                        .originalAmount(t.getOriginalAmount())
                        .currencyCode(t.getCurrency().getCode())
                        .usdEquivalentAmount(t.getUsdEquivalentAmount())
                        .status(t.getStatus())
                        .notes(t.getNotes())
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------
    // Student/employee statistics (section 5)
    // ---------------------------------------------------------------

    private StudentEmployeeStatsDTO buildStats() {
        long activeStudents = studentRepository.countByActiveTrue();
        long activeEmployees = employeeRepository.countByActiveTrue();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : studentRepository.countActiveStudentsGroupedByStatus()) {
            byStatus.put((String) row[0], (Long) row[1]);
        }

        return StudentEmployeeStatsDTO.builder()
                .activeStudentCount(activeStudents)
                .activeEmployeeCount(activeEmployees)
                .activeStudentsByStatus(byStatus)
                .build();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private String monthLabel(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + date.getYear();
    }
}
