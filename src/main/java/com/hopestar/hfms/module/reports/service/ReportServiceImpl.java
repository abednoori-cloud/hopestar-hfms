package com.hopestar.hfms.module.reports.service;

import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import com.hopestar.hfms.module.finance.advance.repository.EmployeeAdvanceRepository;
import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import com.hopestar.hfms.module.finance.loan.repository.EmployeeLoanRepository;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.repository.StudentPaymentRepository;
import com.hopestar.hfms.module.reports.dto.CashFlowReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.EmployeeOutstandingDTO;
import com.hopestar.hfms.module.reports.dto.EmployeeReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.OutstandingStudentDTO;
import com.hopestar.hfms.module.reports.dto.StudentReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.TransactionTypeBreakdownDTO;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import com.hopestar.hfms.module.student.entity.Program;
import com.hopestar.hfms.module.student.entity.StudentContract;
import com.hopestar.hfms.module.student.repository.ProgramRepository;
import com.hopestar.hfms.module.student.repository.StudentContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements {@link ReportService}. Reads directly from existing
 * repositories, mirroring {@code DashboardServiceImpl}'s established
 * "read-only aggregation layer" pattern -- see that class's Javadoc.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    /** The 7 types the Money In/Out report's breakdown table shows, per the report spec. */
    private static final List<TransactionType> CASHFLOW_BREAKDOWN_TYPES = List.of(
            TransactionType.STUDENT_PAYMENT, TransactionType.SALARY, TransactionType.EXPENSE,
            TransactionType.LOAN_DISBURSEMENT, TransactionType.LOAN_REPAYMENT,
            TransactionType.ADVANCE, TransactionType.ADVANCE_REPAYMENT);

    private final TransactionRepository transactionRepository;
    private final StudentPaymentRepository studentPaymentRepository;
    private final StudentContractRepository studentContractRepository;
    private final ProgramRepository programRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeAdvanceRepository employeeAdvanceRepository;

    // ---------------------------------------------------------------
    // Report 1: Money In/Out
    // ---------------------------------------------------------------

    @Override
    public CashFlowReportResponseDTO getCashFlowReport(LocalDate from, LocalDate to) {
        BigDecimal totalIncome = transactionRepository.sumUsdEquivalentAmountByDirectionAndStatusAndDateRange(
                Direction.INCOME, TransactionStatus.POSTED, from, to);
        BigDecimal totalExpense = transactionRepository.sumUsdEquivalentAmountByDirectionAndStatusAndDateRange(
                Direction.EXPENSE, TransactionStatus.POSTED, from, to);

        Map<TransactionType, Object[]> byType = groupTransactionsByType(from, to);

        List<TransactionTypeBreakdownDTO> breakdown = CASHFLOW_BREAKDOWN_TYPES.stream()
                .map(type -> {
                    Object[] row = byType.get(type);
                    return TransactionTypeBreakdownDTO.builder()
                            .transactionType(type)
                            .count(row != null ? (Long) row[1] : 0L)
                            .usdTotal(row != null ? (BigDecimal) row[2] : BigDecimal.ZERO)
                            .build();
                })
                .toList();

        return CashFlowReportResponseDTO.builder()
                .from(from)
                .to(to)
                .totalIncomeUsd(totalIncome)
                .totalExpenseUsd(totalExpense)
                .netPositionUsd(MoneyUtil.subtract(totalIncome, totalExpense))
                .breakdown(breakdown)
                .build();
    }

    // ---------------------------------------------------------------
    // Report 2: Student Report
    // ---------------------------------------------------------------

    /**
     * {@code totalRevenueUsd} and {@code outstandingStudents} are
     * deliberately independent queries with independent scopes: revenue is
     * "what was collected in this period" ({@code paymentDate BETWEEN
     * from/to}); outstanding balance is "what is owed right now" (no date
     * filter at all -- a contract signed two years ago with an unpaid
     * balance is still outstanding today regardless of the selected
     * period). Conflating the two would misrepresent one of them, per the
     * report spec.
     */
    @Override
    public StudentReportResponseDTO getStudentReport(LocalDate from, LocalDate to, Long programId) {
        BigDecimal totalRevenue = studentPaymentRepository.sumUsdEquivalentAmountByStatusAndDateRangeAndProgram(
                PaymentStatus.POSTED, from, to, programId);

        List<Object[]> rows = studentContractRepository.findTopOutstandingContractsWithRemainingBalance(
                ContractStatus.ACTIVE, PaymentStatus.POSTED, programId, Pageable.unpaged());

        List<OutstandingStudentDTO> outstanding = rows.stream()
                .map(row -> {
                    StudentContract contract = (StudentContract) row[0];
                    BigDecimal remaining = (BigDecimal) row[1];
                    return OutstandingStudentDTO.builder()
                            .contractId(contract.getId())
                            .studentId(contract.getStudent().getId())
                            .studentCode(contract.getStudent().getStudentCode())
                            .studentFullName(contract.getStudent().getFullName())
                            .programName(contract.getProgram().getName())
                            .contractUsdEquivalentAmount(contract.getUsdEquivalentAmount())
                            .remainingBalanceUsd(remaining)
                            .build();
                })
                .toList();

        BigDecimal totalOutstanding = outstanding.stream()
                .map(OutstandingStudentDTO::getRemainingBalanceUsd)
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        String programName = programId == null
                ? "All Programs"
                : programRepository.findById(programId).map(Program::getName).orElse("Unknown Program");

        return StudentReportResponseDTO.builder()
                .from(from)
                .to(to)
                .programId(programId)
                .programName(programName)
                .totalRevenueUsd(totalRevenue)
                .totalOutstandingUsd(totalOutstanding)
                .studentsOwingCount(outstanding.size())
                .outstandingStudents(outstanding)
                .build();
    }

    // ---------------------------------------------------------------
    // Report 3: Employee Report
    // ---------------------------------------------------------------

    /**
     * {@code totalPayrollCostUsd} is sourced from {@code POSTED} {@code
     * SALARY}-type ledger transactions, not {@code
     * Salary.usdEquivalentSalary} -- see {@code TransactionRepository
     * .countAndSumUsdEquivalentAmountGroupedByTypeAndStatusAndDateRange}'s
     * Javadoc for why: that field is the basic-salary component only,
     * while the ledger transaction's {@code usdEquivalentAmount} is the
     * USD equivalent of the full {@code netSalary} actually posted.
     */
    @Override
    public EmployeeReportResponseDTO getEmployeeReport(LocalDate from, LocalDate to) {
        Object[] salaryRow = groupTransactionsByType(from, to).get(TransactionType.SALARY);
        long payrollCount = salaryRow != null ? (Long) salaryRow[1] : 0L;
        BigDecimal payrollCost = salaryRow != null ? (BigDecimal) salaryRow[2] : BigDecimal.ZERO;

        List<Object[]> loanRows = employeeLoanRepository.sumRemainingBalanceUsdGroupedByEmployeeAndStatus(LoanStatus.ACTIVE);
        List<Object[]> advanceRows = employeeAdvanceRepository.sumRemainingBalanceUsdGroupedByEmployeeAndStatus(AdvanceStatus.ACTIVE);

        Map<Long, EmployeeOutstandingDTO.EmployeeOutstandingDTOBuilder> byEmployee = new LinkedHashMap<>();
        for (Object[] row : loanRows) {
            builderFor(byEmployee, row).loanOutstandingUsd((BigDecimal) row[3]);
        }
        for (Object[] row : advanceRows) {
            builderFor(byEmployee, row).advanceOutstandingUsd((BigDecimal) row[3]);
        }

        List<EmployeeOutstandingDTO> employees = byEmployee.values().stream()
                .map(builder -> {
                    EmployeeOutstandingDTO dto = builder.build();
                    dto.setTotalOutstandingUsd(MoneyUtil.add(dto.getLoanOutstandingUsd(), dto.getAdvanceOutstandingUsd()));
                    return dto;
                })
                .sorted(Comparator.comparing(EmployeeOutstandingDTO::getTotalOutstandingUsd).reversed())
                .toList();

        BigDecimal totalEmployeeOutstanding = employees.stream()
                .map(EmployeeOutstandingDTO::getTotalOutstandingUsd)
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        return EmployeeReportResponseDTO.builder()
                .from(from)
                .to(to)
                .totalPayrollCostUsd(payrollCost)
                .payrollTransactionCount(payrollCount)
                .totalEmployeeOutstandingUsd(totalEmployeeOutstanding)
                .employeesWithBalances(employees)
                .build();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Map<TransactionType, Object[]> groupTransactionsByType(LocalDate from, LocalDate to) {
        List<Object[]> rows = transactionRepository
                .countAndSumUsdEquivalentAmountGroupedByTypeAndStatusAndDateRange(TransactionStatus.POSTED, from, to);
        Map<TransactionType, Object[]> byType = new EnumMap<>(TransactionType.class);
        for (Object[] row : rows) {
            byType.put((TransactionType) row[0], row);
        }
        return byType;
    }

    /** row = {@code Object[]{Long employeeId, String employeeCode, String employeeFullName, BigDecimal usd}}. */
    private EmployeeOutstandingDTO.EmployeeOutstandingDTOBuilder builderFor(
            Map<Long, EmployeeOutstandingDTO.EmployeeOutstandingDTOBuilder> byEmployee, Object[] row) {
        Long employeeId = (Long) row[0];
        return byEmployee.computeIfAbsent(employeeId, id -> EmployeeOutstandingDTO.builder()
                .employeeId(id)
                .employeeCode((String) row[1])
                .employeeFullName((String) row[2])
                .loanOutstandingUsd(BigDecimal.ZERO)
                .advanceOutstandingUsd(BigDecimal.ZERO));
    }
}
