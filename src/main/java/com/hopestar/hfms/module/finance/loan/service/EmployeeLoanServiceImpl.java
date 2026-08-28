package com.hopestar.hfms.module.finance.loan.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.finance.employee.entity.Employee;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeRepository;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.PostTransactionRequest;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import com.hopestar.hfms.module.finance.ledger.repository.PaymentMethodRepository;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import com.hopestar.hfms.module.finance.ledger.service.LedgerService;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanCreateDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanResponseDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanSearchDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanUpdateDTO;
import com.hopestar.hfms.module.finance.loan.dto.LoanRepaymentCreateDTO;
import com.hopestar.hfms.module.finance.loan.dto.LoanRepaymentResponseDTO;
import com.hopestar.hfms.module.finance.loan.entity.EmployeeLoan;
import com.hopestar.hfms.module.finance.loan.entity.LoanRepaymentSchedule;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import com.hopestar.hfms.module.finance.loan.repository.EmployeeLoanRepository;
import com.hopestar.hfms.module.finance.loan.repository.EmployeeLoanSpecifications;
import com.hopestar.hfms.module.finance.loan.repository.LoanRepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implements {@link EmployeeLoanService}. See that interface's Javadoc
 * for the governing business rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeLoanServiceImpl implements EmployeeLoanService {

    private static final String LOAN_SEQUENCE_KEY = "LOAN";
    private static final String LOAN_CODE_PREFIX = "LOAN";
    private static final int LOAN_CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanRepaymentScheduleRepository loanRepaymentScheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public EmployeeLoanResponseDTO create(EmployeeLoanCreateDTO createDTO) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(createDTO.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", createDTO.getEmployeeId()));

        BigDecimal loanAmount = MoneyUtil.normalize(createDTO.getLoanAmount());
        BigDecimal monthlyDeduction = MoneyUtil.normalize(createDTO.getMonthlyDeduction());
        requireDeductionWithinLoanAmount(monthlyDeduction, loanAmount);

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(loanAmount, exchangeRate);

        String loanNumber = sequenceGeneratorService.nextYearlyValue(
                LOAN_SEQUENCE_KEY, LOAN_CODE_PREFIX, LOAN_CODE_PADDING, null);

        EmployeeLoan loan = EmployeeLoan.builder()
                .loanNumber(loanNumber)
                .employee(employee)
                .loanAmount(loanAmount)
                .currency(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .monthlyDeduction(monthlyDeduction)
                .remainingBalance(loanAmount)
                .loanDate(createDTO.getLoanDate())
                .startMonth(createDTO.getStartMonth())
                .startYear(createDTO.getStartYear())
                .endMonth(createDTO.getEndMonth())
                .endYear(createDTO.getEndYear())
                .status(LoanStatus.DRAFT)
                .remarks(createDTO.getRemarks())
                .build();

        EmployeeLoan saved = employeeLoanRepository.save(loan);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public EmployeeLoanResponseDTO update(Long loanId, EmployeeLoanUpdateDTO updateDTO) {
        EmployeeLoan loan = getActiveLoanEntity(loanId);
        requireDraft(loan);

        BigDecimal loanAmount = MoneyUtil.normalize(updateDTO.getLoanAmount());
        BigDecimal monthlyDeduction = MoneyUtil.normalize(updateDTO.getMonthlyDeduction());
        requireDeductionWithinLoanAmount(monthlyDeduction, loanAmount);

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(updateDTO.getCurrency(), updateDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(loanAmount, exchangeRate);

        loan.setLoanAmount(loanAmount);
        loan.setCurrency(updateDTO.getCurrency());
        loan.setExchangeRateToUsd(exchangeRate);
        loan.setUsdEquivalentAmount(usdEquivalentAmount);
        loan.setMonthlyDeduction(monthlyDeduction);
        // Safe to reset outright: a DRAFT loan can never have had a
        // repayment posted against it (repayments require ACTIVE).
        loan.setRemainingBalance(loanAmount);
        loan.setLoanDate(updateDTO.getLoanDate());
        loan.setStartMonth(updateDTO.getStartMonth());
        loan.setStartYear(updateDTO.getStartYear());
        loan.setEndMonth(updateDTO.getEndMonth());
        loan.setEndYear(updateDTO.getEndYear());
        loan.setRemarks(updateDTO.getRemarks());

        return toResponseDTO(loan);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public EmployeeLoanResponseDTO disburse(Long loanId, Long paymentMethodId) {
        EmployeeLoan loan = getActiveLoanEntity(loanId);

        if (loan.getStatus() != LoanStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Loan " + loan.getLoanNumber() + " can only be disbursed while DRAFT (currently "
                            + loan.getStatus() + ").");
        }

        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.LOAN_DISBURSEMENT)
                .originalAmount(loan.getLoanAmount())
                .currencyCode(loan.getCurrency())
                .exchangeRateToUsd(loan.getExchangeRateToUsd())
                .paymentMethodId(paymentMethodId)
                .transactionDate(loan.getLoanDate())
                .referenceTable("employee_loans")
                .referenceId(loan.getId())
                .branchId(loan.getEmployee().getBranch() != null ? loan.getEmployee().getBranch().getId() : null)
                .notes("Disbursement of loan " + loan.getLoanNumber() + " to "
                        + loan.getEmployee().getEmployeeCode())
                .build();

        // LedgerService remains the only creator of Transaction rows --
        // this method never constructs one itself.
        TransactionResponseDTO postedTransaction = ledgerService.postExpense(request);
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());

        loan.setLedgerTransaction(transactionReference);
        loan.setStatus(LoanStatus.ACTIVE);

        return toResponseDTO(loan);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public EmployeeLoanResponseDTO voidLoan(Long loanId, String reason) {
        EmployeeLoan loan = getActiveLoanEntity(loanId);

        if (loan.getStatus() == LoanStatus.VOID) {
            throw new BusinessValidationException("Loan " + loan.getLoanNumber() + " is already VOID.");
        }
        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessValidationException(
                    "Loan " + loan.getLoanNumber() + " is fully repaid and can no longer be voided.");
        }

        if (loan.getStatus() == LoanStatus.ACTIVE && loan.getLedgerTransaction() != null) {
            // Voiding, never mutating, the ledger row directly -- LedgerService
            // remains the only class that changes a Transaction's status.
            ledgerService.voidTransaction(loan.getLedgerTransaction().getId(), reason);
        }

        loan.setStatus(LoanStatus.VOID);
        return toResponseDTO(loan);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('LOAN_MANAGE')")
    public LoanRepaymentResponseDTO recordRepayment(Long loanId, LoanRepaymentCreateDTO createDTO) {
        EmployeeLoan loan = getActiveLoanEntity(loanId);

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Loan " + loan.getLoanNumber() + " does not accept repayments while " + loan.getStatus()
                            + " (must be ACTIVE).");
        }

        BigDecimal amount = MoneyUtil.normalize(createDTO.getAmount());
        if (MoneyUtil.isNegative(MoneyUtil.subtract(loan.getRemainingBalance(), amount))) {
            throw new BusinessValidationException(
                    "Repayment amount " + amount + " exceeds the remaining balance "
                            + loan.getRemainingBalance() + " on loan " + loan.getLoanNumber() + ".");
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(createDTO.getPaymentMethodId());

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(amount, exchangeRate);

        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.LOAN_REPAYMENT)
                .originalAmount(amount)
                .currencyCode(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .paymentMethodId(createDTO.getPaymentMethodId())
                .transactionDate(createDTO.getRepaymentDate())
                .referenceTable("loan_repayments")
                .referenceId(loan.getId())
                .branchId(loan.getEmployee().getBranch() != null ? loan.getEmployee().getBranch().getId() : null)
                .notes("Repayment against loan " + loan.getLoanNumber() + " by "
                        + loan.getEmployee().getEmployeeCode())
                .build();

        // LedgerService remains the only creator of Transaction rows --
        // this method never constructs one itself.
        TransactionResponseDTO postedTransaction = ledgerService.postIncome(request);
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());

        LoanRepaymentSchedule repayment = LoanRepaymentSchedule.builder()
                .loan(loan)
                .repaymentDate(createDTO.getRepaymentDate())
                .amount(amount)
                .currency(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .paymentMethod(paymentMethod)
                .ledgerTransaction(transactionReference)
                .notes(createDTO.getNotes())
                .build();
        LoanRepaymentSchedule savedRepayment = loanRepaymentScheduleRepository.save(repayment);

        BigDecimal newBalance = MoneyUtil.subtract(loan.getRemainingBalance(), amount);
        loan.setRemainingBalance(newBalance);
        if (MoneyUtil.isZero(newBalance)) {
            loan.setStatus(LoanStatus.CLOSED);
        }

        return toRepaymentResponseDTO(savedRepayment);
    }

    @Override
    public EmployeeLoanResponseDTO getById(Long loanId) {
        return toResponseDTO(getActiveLoanEntity(loanId));
    }

    @Override
    public EmployeeLoanResponseDTO getByLoanNumber(String loanNumber) {
        EmployeeLoan loan = employeeLoanRepository.findByLoanNumberAndActiveTrue(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanNumber));
        return toResponseDTO(loan);
    }

    @Override
    public PageResponse<EmployeeLoanResponseDTO> search(EmployeeLoanSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        String sortField = EmployeeLoanSpecifications.resolveSortField(searchDTO.getSortBy());
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDTO.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<EmployeeLoan> result = employeeLoanRepository.findAll(
                EmployeeLoanSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(direction, sortField)));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    public List<EmployeeLoanResponseDTO> listByEmployee(Long employeeId) {
        return employeeLoanRepository.findByEmployeeIdAndActiveTrueOrderByLoanDateDesc(employeeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<LoanRepaymentResponseDTO> listRepayments(Long loanId) {
        return loanRepaymentScheduleRepository.findByLoanIdAndActiveTrueOrderByRepaymentDateDesc(loanId).stream()
                .map(this::toRepaymentResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private EmployeeLoan getActiveLoanEntity(Long loanId) {
        return employeeLoanRepository.findByIdAndActiveTrue(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
    }

    private void requireDraft(EmployeeLoan loan) {
        if (loan.getStatus() != LoanStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Loan " + loan.getLoanNumber() + " can no longer be edited because it is "
                            + loan.getStatus() + ", not DRAFT.");
        }
    }

    private void requireDeductionWithinLoanAmount(BigDecimal monthlyDeduction, BigDecimal loanAmount) {
        if (MoneyUtil.isNegative(MoneyUtil.subtract(loanAmount, monthlyDeduction))) {
            throw new BusinessValidationException(
                    "Monthly deduction (" + monthlyDeduction + ") cannot exceed the loan amount (" + loanAmount + ").");
        }
    }

    private PaymentMethod resolvePaymentMethod(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method", paymentMethodId));
    }

    private EmployeeLoanResponseDTO toResponseDTO(EmployeeLoan loan) {
        Employee employee = loan.getEmployee();

        return EmployeeLoanResponseDTO.builder()
                .id(loan.getId())
                .active(loan.isActive())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .createdBy(loan.getCreatedBy())
                .updatedBy(loan.getUpdatedBy())
                .loanNumber(loan.getLoanNumber())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeFullName(employee.getFullName())
                .loanAmount(loan.getLoanAmount())
                .currency(loan.getCurrency())
                .exchangeRateToUsd(loan.getExchangeRateToUsd())
                .usdEquivalentAmount(loan.getUsdEquivalentAmount())
                .monthlyDeduction(loan.getMonthlyDeduction())
                .remainingBalance(loan.getRemainingBalance())
                .loanDate(loan.getLoanDate())
                .startMonth(loan.getStartMonth())
                .startYear(loan.getStartYear())
                .endMonth(loan.getEndMonth())
                .endYear(loan.getEndYear())
                .status(loan.getStatus())
                .remarks(loan.getRemarks())
                .ledgerTransactionId(loan.getLedgerTransaction() != null ? loan.getLedgerTransaction().getId() : null)
                .ledgerTransactionCode(loan.getLedgerTransaction() != null
                        ? ledgerService.findTransaction(loan.getLedgerTransaction().getId()).getTransactionCode()
                        : null)
                .build();
    }

    private LoanRepaymentResponseDTO toRepaymentResponseDTO(LoanRepaymentSchedule repayment) {
        PaymentMethodResponseDTO paymentMethodDTO = PaymentMethodResponseDTO.builder()
                .id(repayment.getPaymentMethod().getId())
                .name(repayment.getPaymentMethod().getName())
                .code(repayment.getPaymentMethod().getCode())
                .description(repayment.getPaymentMethod().getDescription())
                .build();

        return LoanRepaymentResponseDTO.builder()
                .loanId(repayment.getLoan().getId())
                .loanNumber(repayment.getLoan().getLoanNumber())
                .repaymentDate(repayment.getRepaymentDate())
                .amount(repayment.getAmount())
                .currency(repayment.getCurrency())
                .exchangeRateToUsd(repayment.getExchangeRateToUsd())
                .usdEquivalentAmount(repayment.getUsdEquivalentAmount())
                .paymentMethod(paymentMethodDTO)
                .ledgerTransactionId(repayment.getLedgerTransaction() != null ? repayment.getLedgerTransaction().getId() : null)
                .ledgerTransactionCode(repayment.getLedgerTransaction() != null
                        ? ledgerService.findTransaction(repayment.getLedgerTransaction().getId()).getTransactionCode()
                        : null)
                .notes(repayment.getNotes())
                .build();
    }
}
