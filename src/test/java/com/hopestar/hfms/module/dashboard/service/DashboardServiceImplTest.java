package com.hopestar.hfms.module.dashboard.service;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.dashboard.dto.DashboardSummaryDTO;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import com.hopestar.hfms.module.finance.advance.repository.EmployeeAdvanceRepository;
import com.hopestar.hfms.module.finance.employee.entity.Employee;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeRepository;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import com.hopestar.hfms.module.finance.expense.repository.ExpenseRepository;
import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import com.hopestar.hfms.module.finance.loan.repository.EmployeeLoanRepository;
import com.hopestar.hfms.module.finance.salary.entity.Salary;
import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import com.hopestar.hfms.module.finance.salary.repository.SalaryRepository;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.repository.StudentPaymentRepository;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import com.hopestar.hfms.module.student.entity.Student;
import com.hopestar.hfms.module.student.entity.StudentContract;
import com.hopestar.hfms.module.student.repository.StudentContractRepository;
import com.hopestar.hfms.module.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Dashboard's (Module 2) read-only aggregation logic.
 * All repositories are mocked -- no Spring context, no database -- so
 * these exercise {@link DashboardServiceImpl}'s own arithmetic and
 * mapping, the same way {@code AuthServiceImplTest} exercises {@code
 * AuthServiceImpl} in isolation.
 * <p>
 * {@link #setUp()} stubs every repository call to a safe zero/empty
 * default with {@code lenient()} (each test only cares about a subset).
 * Individual tests then override the specific calls they're asserting on
 * -- Mockito uses the most-recently-registered matching stub, so those
 * per-test overrides always win over the shared defaults.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private StudentContractRepository studentContractRepository;
    @Mock private StudentPaymentRepository studentPaymentRepository;
    @Mock private SalaryRepository salaryRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeLoanRepository employeeLoanRepository;
    @Mock private EmployeeAdvanceRepository employeeAdvanceRepository;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionRepository.sumUsdEquivalentAmountByDirectionAndStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(transactionRepository.sumUsdEquivalentAmountByDirectionAndStatusAndDateRange(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(transactionRepository.findByStatusAndActiveTrueWithCurrencyOrderByDateDesc(any(), any()))
                .thenReturn(List.of());

        lenient().when(studentContractRepository.sumUsdEquivalentAmountByStatusAndActiveTrue(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(studentContractRepository.countDistinctStudentsWithOutstandingBalance(any(), any()))
                .thenReturn(0L);
        lenient().when(studentContractRepository.findTopOutstandingContractsWithRemainingBalance(any(), any(), any()))
                .thenReturn(List.of());

        lenient().when(studentPaymentRepository.sumUsdEquivalentAmountByStatusAndContractStatus(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        lenient().when(salaryRepository.countByPaymentStatusAndActiveTrue(any())).thenReturn(0L);
        lenient().when(salaryRepository.sumUsdEquivalentSalaryByPaymentStatusAndActiveTrue(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(salaryRepository.countByPaymentStatusAndMonthAndYearAndActiveTrue(any(), anyInt(), anyInt()))
                .thenReturn(0L);
        lenient().when(salaryRepository.findByPaymentStatusAndActiveTrueWithEmployeeOrderByYearDescMonthDesc(any(), any()))
                .thenReturn(List.of());

        lenient().when(studentRepository.countByActiveTrue()).thenReturn(0L);
        lenient().when(studentRepository.countActiveStudentsGroupedByStatus()).thenReturn(List.of());
        lenient().when(employeeRepository.countByActiveTrue()).thenReturn(0L);

        lenient().when(employeeLoanRepository.countByStatusAndActiveTrue(any())).thenReturn(0L);
        lenient().when(employeeLoanRepository.sumRemainingBalanceUsdByStatusAndActiveTrue(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(employeeAdvanceRepository.countByStatusAndActiveTrue(any())).thenReturn(0L);
        lenient().when(employeeAdvanceRepository.sumRemainingBalanceUsdByStatusAndActiveTrue(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(expenseRepository.sumUsdEquivalentAmountByStatusAndDateRange(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    private Student student(long id, String code, String name) {
        return Student.builder().id(id).studentCode(code).fullName(name).build();
    }

    private StudentContract contract(long id, Student owner, BigDecimal usdEquivalent) {
        return StudentContract.builder()
                .id(id)
                .student(owner)
                .usdEquivalentAmount(usdEquivalent)
                .status(ContractStatus.ACTIVE)
                .build();
    }

    private Currency usdCurrency() {
        return Currency.builder().id(1L).code("USD").name("US Dollar").symbol("$").baseCurrency(true).build();
    }

    private Transaction transaction(String code, Direction direction, BigDecimal usdAmount) {
        return Transaction.builder()
                .transactionCode(code)
                .transactionType(TransactionType.STUDENT_PAYMENT)
                .direction(direction)
                .originalAmount(usdAmount)
                .currency(usdCurrency())
                .usdEquivalentAmount(usdAmount)
                .transactionDate(LocalDate.now())
                .status(TransactionStatus.POSTED)
                .build();
    }

    private Employee employee(long id, String code, String name) {
        return Employee.builder().id(id).employeeCode(code).fullName(name).build();
    }

    private Salary salary(long id, Employee owner, int month, int year, BigDecimal net) {
        return Salary.builder()
                .id(id)
                .employee(owner)
                .month(month)
                .year(year)
                .netSalary(net)
                .currency(SupportedCurrency.USD)
                .paymentStatus(SalaryPaymentStatus.DRAFT)
                .build();
    }

    // ---------------------------------------------------------------
    // Financial summary
    // ---------------------------------------------------------------

    @Test
    void financialSummary_computesNetPositionFromPostedIncomeAndExpense() {
        when(transactionRepository.sumUsdEquivalentAmountByDirectionAndStatus(Direction.INCOME, TransactionStatus.POSTED))
                .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumUsdEquivalentAmountByDirectionAndStatus(Direction.EXPENSE, TransactionStatus.POSTED))
                .thenReturn(new BigDecimal("4000.00"));

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();

        assertThat(summary.getFinancialSummary().getTotalIncomeUsd()).isEqualByComparingTo("10000.00");
        assertThat(summary.getFinancialSummary().getTotalExpenseUsd()).isEqualByComparingTo("4000.00");
        assertThat(summary.getFinancialSummary().getNetPositionUsd()).isEqualByComparingTo("6000.00");
        assertThat(summary.getFinancialSummary().getCurrentPeriodLabel()).isNotBlank();
    }

    // ---------------------------------------------------------------
    // Outstanding student receivables
    // ---------------------------------------------------------------

    @Test
    void outstandingReceivables_excludesFullyPaidContracts_includesOwingOnes() {
        when(studentContractRepository.sumUsdEquivalentAmountByStatusAndActiveTrue(ContractStatus.ACTIVE))
                .thenReturn(new BigDecimal("5000.00"));
        when(studentPaymentRepository.sumUsdEquivalentAmountByStatusAndContractStatus(PaymentStatus.POSTED, ContractStatus.ACTIVE))
                .thenReturn(new BigDecimal("3000.00"));
        when(studentContractRepository.countDistinctStudentsWithOutstandingBalance(ContractStatus.ACTIVE, PaymentStatus.POSTED))
                .thenReturn(2L);

        Student owingStudent = student(1L, "STU-0001", "Alice Owes");
        StudentContract owingContract = contract(10L, owingStudent, new BigDecimal("1000.00"));

        // The fully-paid contract never appears here: the repository query
        // itself filters to a positive remaining balance, so only owing
        // contracts are ever returned as rows.
        when(studentContractRepository.findTopOutstandingContractsWithRemainingBalance(
                any(ContractStatus.class), any(PaymentStatus.class), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[] {owingContract, new BigDecimal("600.00")}));

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();

        assertThat(summary.getOutstandingReceivables().getTotalOutstandingUsd()).isEqualByComparingTo("2000.00");
        assertThat(summary.getOutstandingReceivables().getStudentsWithOutstandingBalanceCount()).isEqualTo(2L);
        assertThat(summary.getOutstandingReceivables().getTopOutstandingContracts()).hasSize(1);
        assertThat(summary.getOutstandingReceivables().getTopOutstandingContracts().get(0).getStudentFullName())
                .isEqualTo("Alice Owes");
        assertThat(summary.getOutstandingReceivables().getTopOutstandingContracts().get(0).getRemainingBalanceUsd())
                .isEqualByComparingTo("600.00");
    }

    // ---------------------------------------------------------------
    // Salary overview
    // ---------------------------------------------------------------

    @Test
    void salaryOverview_reportsPendingCountAndBasicPay() {
        when(salaryRepository.countByPaymentStatusAndActiveTrue(SalaryPaymentStatus.DRAFT)).thenReturn(3L);
        when(salaryRepository.sumUsdEquivalentSalaryByPaymentStatusAndActiveTrue(SalaryPaymentStatus.DRAFT))
                .thenReturn(new BigDecimal("1500.00"));
        when(salaryRepository.countByPaymentStatusAndMonthAndYearAndActiveTrue(
                eq(SalaryPaymentStatus.DRAFT), anyInt(), anyInt())).thenReturn(2L);
        when(salaryRepository.countByPaymentStatusAndMonthAndYearAndActiveTrue(
                eq(SalaryPaymentStatus.POSTED), anyInt(), anyInt())).thenReturn(5L);

        Employee emp = employee(1L, "EMP-0001", "Jane Employee");
        Salary pending = salary(20L, emp, 8, 2026, new BigDecimal("900.00"));
        when(salaryRepository.findByPaymentStatusAndActiveTrueWithEmployeeOrderByYearDescMonthDesc(
                eq(SalaryPaymentStatus.DRAFT), any(Pageable.class))).thenReturn(List.of(pending));

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();

        assertThat(summary.getSalaryOverview().getPendingSalaryCount()).isEqualTo(3L);
        assertThat(summary.getSalaryOverview().getPendingBasicPayUsd()).isEqualByComparingTo("1500.00");
        assertThat(summary.getSalaryOverview().getCurrentPeriodDraftCount()).isEqualTo(2L);
        assertThat(summary.getSalaryOverview().getCurrentPeriodPostedCount()).isEqualTo(5L);
        assertThat(summary.getSalaryOverview().getCurrentPeriodTotalCount()).isEqualTo(7L);
        assertThat(summary.getSalaryOverview().getPendingSalaries()).hasSize(1);
        assertThat(summary.getSalaryOverview().getPendingSalaries().get(0).getEmployeeFullName()).isEqualTo("Jane Employee");
    }

    // ---------------------------------------------------------------
    // Loans / advances / expenses
    // ---------------------------------------------------------------

    @Test
    void loanAdvanceExpenseSummary_reportsActiveCountsAndOutstandingAndMonthExpense() {
        when(employeeLoanRepository.countByStatusAndActiveTrue(LoanStatus.ACTIVE)).thenReturn(2L);
        when(employeeLoanRepository.sumRemainingBalanceUsdByStatusAndActiveTrue(LoanStatus.ACTIVE))
                .thenReturn(new BigDecimal("520.02"));
        when(employeeAdvanceRepository.countByStatusAndActiveTrue(AdvanceStatus.ACTIVE)).thenReturn(1L);
        when(employeeAdvanceRepository.sumRemainingBalanceUsdByStatusAndActiveTrue(AdvanceStatus.ACTIVE))
                .thenReturn(new BigDecimal("300.00"));
        when(expenseRepository.sumUsdEquivalentAmountByStatusAndDateRange(eq(ExpenseStatus.POSTED), any(), any()))
                .thenReturn(new BigDecimal("150.00"));

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();

        assertThat(summary.getLoanAdvanceExpenseSummary().getActiveLoanCount()).isEqualTo(2L);
        assertThat(summary.getLoanAdvanceExpenseSummary().getLoanOutstandingUsd()).isEqualByComparingTo("520.02");
        assertThat(summary.getLoanAdvanceExpenseSummary().getActiveAdvanceCount()).isEqualTo(1L);
        assertThat(summary.getLoanAdvanceExpenseSummary().getAdvanceOutstandingUsd()).isEqualByComparingTo("300.00");
        assertThat(summary.getLoanAdvanceExpenseSummary().getCurrentPeriodExpenseUsd()).isEqualByComparingTo("150.00");
        assertThat(summary.getLoanAdvanceExpenseSummary().getCurrentPeriodLabel()).isNotBlank();
    }

    // ---------------------------------------------------------------
    // Recent transactions
    // ---------------------------------------------------------------

    @Test
    void recentTransactions_mapsDirectionAndCurrencyCorrectly() {
        Transaction income = transaction("TXN-2026-000001", Direction.INCOME, new BigDecimal("500.00"));
        when(transactionRepository.findByStatusAndActiveTrueWithCurrencyOrderByDateDesc(
                eq(TransactionStatus.POSTED), any(Pageable.class))).thenReturn(List.of(income));

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();

        assertThat(summary.getRecentTransactions()).hasSize(1);
        assertThat(summary.getRecentTransactions().get(0).getTransactionCode()).isEqualTo("TXN-2026-000001");
        assertThat(summary.getRecentTransactions().get(0).getDirection()).isEqualTo(Direction.INCOME);
        assertThat(summary.getRecentTransactions().get(0).getCurrencyCode()).isEqualTo("USD");
    }

    // ---------------------------------------------------------------
    // Empty-data behavior
    // ---------------------------------------------------------------

    @Test
    void emptyData_producesZeroedSummaryWithoutErrors() {
        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();

        assertThat(summary.getFinancialSummary().getTotalIncomeUsd()).isEqualByComparingTo("0.00");
        assertThat(summary.getFinancialSummary().getNetPositionUsd()).isEqualByComparingTo("0.00");
        assertThat(summary.getOutstandingReceivables().getTotalOutstandingUsd()).isEqualByComparingTo("0.00");
        assertThat(summary.getOutstandingReceivables().getTopOutstandingContracts()).isEmpty();
        assertThat(summary.getSalaryOverview().getPendingSalaries()).isEmpty();
        assertThat(summary.getLoanAdvanceExpenseSummary().getActiveLoanCount()).isZero();
        assertThat(summary.getLoanAdvanceExpenseSummary().getLoanOutstandingUsd()).isEqualByComparingTo("0.00");
        assertThat(summary.getLoanAdvanceExpenseSummary().getActiveAdvanceCount()).isZero();
        assertThat(summary.getLoanAdvanceExpenseSummary().getAdvanceOutstandingUsd()).isEqualByComparingTo("0.00");
        assertThat(summary.getLoanAdvanceExpenseSummary().getCurrentPeriodExpenseUsd()).isEqualByComparingTo("0.00");
        assertThat(summary.getRecentTransactions()).isEmpty();
        assertThat(summary.getStats().getActiveStudentCount()).isZero();
        assertThat(summary.getStats().getActiveEmployeeCount()).isZero();
        assertThat(summary.getStats().getActiveStudentsByStatus()).isEmpty();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
