package com.hopestar.hfms.module.finance.salary.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.DuplicateResourceException;
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
import com.hopestar.hfms.module.finance.salary.dto.SalaryCreateDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalaryResponseDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalarySearchDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalaryUpdateDTO;
import com.hopestar.hfms.module.finance.salary.entity.Salary;
import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import com.hopestar.hfms.module.finance.salary.repository.SalaryRepository;
import com.hopestar.hfms.module.finance.salary.repository.SalarySpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Implements {@link SalaryService}. See that interface's Javadoc for the
 * governing business rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryServiceImpl implements SalaryService {

    private static final String SALARY_SEQUENCE_KEY = "SALARY";
    private static final String SALARY_CODE_PREFIX = "SAL";
    private static final int SALARY_CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final SalaryRepository salaryRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    public SalaryResponseDTO create(SalaryCreateDTO createDTO) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(createDTO.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", createDTO.getEmployeeId()));

        if (salaryRepository.existsByEmployeeIdAndMonthAndYear(
                createDTO.getEmployeeId(), createDTO.getMonth(), createDTO.getYear())) {
            throw new DuplicateResourceException(
                    "A salary record for employee %s already exists for %d/%d."
                            .formatted(employee.getEmployeeCode(), createDTO.getMonth(), createDTO.getYear()));
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(createDTO.getPaymentMethodId());

        BigDecimal basicSalary = MoneyUtil.normalize(createDTO.getBasicSalary());
        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentSalary = MoneyUtil.multiply(basicSalary, exchangeRate);

        BigDecimal netSalary = computeNetSalary(
                basicSalary, createDTO.getBonus(), createDTO.getOvertimeAmount(), createDTO.getAllowance(),
                createDTO.getPenalty(), createDTO.getLoanDeduction(), createDTO.getAdvanceDeduction(), createDTO.getOtherDeduction());

        String salaryNumber = sequenceGeneratorService.nextYearlyValue(
                SALARY_SEQUENCE_KEY, SALARY_CODE_PREFIX, SALARY_CODE_PADDING, null);

        Salary salary = Salary.builder()
                .salaryNumber(salaryNumber)
                .employee(employee)
                .month(createDTO.getMonth())
                .year(createDTO.getYear())
                .basicSalary(basicSalary)
                .currency(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentSalary(usdEquivalentSalary)
                .bonus(MoneyUtil.normalize(createDTO.getBonus()))
                .overtimeAmount(MoneyUtil.normalize(createDTO.getOvertimeAmount()))
                .allowance(MoneyUtil.normalize(createDTO.getAllowance()))
                .penalty(MoneyUtil.normalize(createDTO.getPenalty()))
                .loanDeduction(MoneyUtil.normalize(createDTO.getLoanDeduction()))
                .advanceDeduction(MoneyUtil.normalize(createDTO.getAdvanceDeduction()))
                .otherDeduction(MoneyUtil.normalize(createDTO.getOtherDeduction()))
                .netSalary(netSalary)
                .paymentStatus(SalaryPaymentStatus.DRAFT)
                .paymentMethod(paymentMethod)
                .remarks(createDTO.getRemarks())
                .build();

        Salary saved = salaryRepository.save(salary);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    public SalaryResponseDTO update(Long salaryId, SalaryUpdateDTO updateDTO) {
        Salary salary = getActiveSalaryEntity(salaryId);
        requireDraft(salary);

        PaymentMethod paymentMethod = resolvePaymentMethod(updateDTO.getPaymentMethodId());

        BigDecimal basicSalary = MoneyUtil.normalize(updateDTO.getBasicSalary());
        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(updateDTO.getCurrency(), updateDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentSalary = MoneyUtil.multiply(basicSalary, exchangeRate);

        BigDecimal netSalary = computeNetSalary(
                basicSalary, updateDTO.getBonus(), updateDTO.getOvertimeAmount(), updateDTO.getAllowance(),
                updateDTO.getPenalty(), updateDTO.getLoanDeduction(), updateDTO.getAdvanceDeduction(), updateDTO.getOtherDeduction());

        salary.setBasicSalary(basicSalary);
        salary.setCurrency(updateDTO.getCurrency());
        salary.setExchangeRateToUsd(exchangeRate);
        salary.setUsdEquivalentSalary(usdEquivalentSalary);
        salary.setBonus(MoneyUtil.normalize(updateDTO.getBonus()));
        salary.setOvertimeAmount(MoneyUtil.normalize(updateDTO.getOvertimeAmount()));
        salary.setAllowance(MoneyUtil.normalize(updateDTO.getAllowance()));
        salary.setPenalty(MoneyUtil.normalize(updateDTO.getPenalty()));
        salary.setLoanDeduction(MoneyUtil.normalize(updateDTO.getLoanDeduction()));
        salary.setAdvanceDeduction(MoneyUtil.normalize(updateDTO.getAdvanceDeduction()));
        salary.setOtherDeduction(MoneyUtil.normalize(updateDTO.getOtherDeduction()));
        salary.setNetSalary(netSalary);
        salary.setPaymentMethod(paymentMethod);
        salary.setRemarks(updateDTO.getRemarks());

        return toResponseDTO(salary);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    public SalaryResponseDTO post(Long salaryId) {
        Salary salary = getActiveSalaryEntity(salaryId);
        requireDraft(salary);

        // Recompute one last time from the persisted components before
        // posting -- net salary must never be entered manually, and this
        // guarantees the ledger amount matches exactly what is stored.
        BigDecimal netSalary = computeNetSalary(
                salary.getBasicSalary(), salary.getBonus(), salary.getOvertimeAmount(), salary.getAllowance(),
                salary.getPenalty(), salary.getLoanDeduction(), salary.getAdvanceDeduction(), salary.getOtherDeduction());
        salary.setNetSalary(netSalary);

        LocalDate postingDate = LocalDate.now();

        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.SALARY)
                .originalAmount(netSalary)
                .currencyCode(salary.getCurrency())
                .exchangeRateToUsd(salary.getExchangeRateToUsd())
                .paymentMethodId(salary.getPaymentMethod().getId())
                .transactionDate(postingDate)
                .referenceTable("salaries")
                .referenceId(salary.getId())
                .branchId(salary.getEmployee().getBranch() != null ? salary.getEmployee().getBranch().getId() : null)
                .notes("Salary " + salary.getSalaryNumber() + " for " + salary.getMonth() + "/" + salary.getYear()
                        + " (" + salary.getEmployee().getEmployeeCode() + ")")
                .build();

        // LedgerService remains the only creator of Transaction rows --
        // this method never constructs one itself.
        TransactionResponseDTO postedTransaction = ledgerService.postExpense(request);
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());

        salary.setLedgerTransaction(transactionReference);
        salary.setPaymentDate(postingDate);
        salary.setPaymentStatus(SalaryPaymentStatus.POSTED);

        return toResponseDTO(salary);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SALARY_APPROVE')")
    public SalaryResponseDTO voidSalary(Long salaryId, String reason) {
        Salary salary = getActiveSalaryEntity(salaryId);

        if (salary.getPaymentStatus() == SalaryPaymentStatus.VOID) {
            throw new BusinessValidationException("Salary " + salary.getSalaryNumber() + " is already VOID.");
        }

        if (salary.getPaymentStatus() == SalaryPaymentStatus.POSTED && salary.getLedgerTransaction() != null) {
            // Voiding, never mutating, the ledger row directly -- LedgerService
            // remains the only class that changes a Transaction's status.
            ledgerService.voidTransaction(salary.getLedgerTransaction().getId(), reason);
        }

        salary.setPaymentStatus(SalaryPaymentStatus.VOID);
        return toResponseDTO(salary);
    }

    @Override
    public SalaryResponseDTO getById(Long salaryId) {
        return toResponseDTO(getActiveSalaryEntity(salaryId));
    }

    @Override
    public SalaryResponseDTO getBySalaryNumber(String salaryNumber) {
        Salary salary = salaryRepository.findBySalaryNumberAndActiveTrue(salaryNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Salary", salaryNumber));
        return toResponseDTO(salary);
    }

    @Override
    public PageResponse<SalaryResponseDTO> search(SalarySearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        String sortField = SalarySpecifications.resolveSortField(searchDTO.getSortBy());
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDTO.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<Salary> result = salaryRepository.findAll(
                SalarySpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(direction, sortField)));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    public List<SalaryResponseDTO> listByEmployee(Long employeeId) {
        return salaryRepository.findByEmployeeIdAndActiveTrueOrderByYearDescMonthDesc(employeeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Salary getActiveSalaryEntity(Long salaryId) {
        return salaryRepository.findByIdAndActiveTrue(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary", salaryId));
    }

    private void requireDraft(Salary salary) {
        if (salary.getPaymentStatus() != SalaryPaymentStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Salary " + salary.getSalaryNumber() + " can no longer be edited because it is "
                            + salary.getPaymentStatus() + ", not DRAFT.");
        }
    }

    private PaymentMethod resolvePaymentMethod(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method", paymentMethodId));
    }

    /**
     * The single implementation of the net-salary formula: {@code
     * basicSalary + bonus + overtimeAmount + allowance - penalty -
     * loanDeduction - advanceDeduction - otherDeduction}. Called from
     * {@code create}, {@code update}, and {@code post} (post recomputes
     * once more from persisted values immediately before posting) so the
     * formula is never duplicated across those three call sites.
     */
    private BigDecimal computeNetSalary(BigDecimal basicSalary, BigDecimal bonus, BigDecimal overtimeAmount,
                                         BigDecimal allowance, BigDecimal penalty, BigDecimal loanDeduction,
                                         BigDecimal advanceDeduction, BigDecimal otherDeduction) {
        BigDecimal earnings = MoneyUtil.add(MoneyUtil.add(basicSalary, bonus), MoneyUtil.add(overtimeAmount, allowance));
        BigDecimal deductions = MoneyUtil.add(MoneyUtil.add(penalty, loanDeduction),
                MoneyUtil.add(advanceDeduction, otherDeduction));
        BigDecimal net = MoneyUtil.subtract(earnings, deductions);

        if (MoneyUtil.isNegative(net)) {
            throw new BusinessValidationException(
                    "Computed net salary is negative (deductions exceed earnings). Please review the amounts entered.");
        }
        return net;
    }

    private SalaryResponseDTO toResponseDTO(Salary salary) {
        Employee employee = salary.getEmployee();

        PaymentMethodResponseDTO paymentMethodDTO = PaymentMethodResponseDTO.builder()
                .id(salary.getPaymentMethod().getId())
                .name(salary.getPaymentMethod().getName())
                .code(salary.getPaymentMethod().getCode())
                .description(salary.getPaymentMethod().getDescription())
                .build();

        return SalaryResponseDTO.builder()
                .id(salary.getId())
                .active(salary.isActive())
                .createdAt(salary.getCreatedAt())
                .updatedAt(salary.getUpdatedAt())
                .createdBy(salary.getCreatedBy())
                .updatedBy(salary.getUpdatedBy())
                .salaryNumber(salary.getSalaryNumber())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeFullName(employee.getFullName())
                .employeeDepartment(employee.getDepartment())
                .month(salary.getMonth())
                .year(salary.getYear())
                .basicSalary(salary.getBasicSalary())
                .currency(salary.getCurrency())
                .exchangeRateToUsd(salary.getExchangeRateToUsd())
                .usdEquivalentSalary(salary.getUsdEquivalentSalary())
                .bonus(salary.getBonus())
                .overtimeAmount(salary.getOvertimeAmount())
                .allowance(salary.getAllowance())
                .penalty(salary.getPenalty())
                .loanDeduction(salary.getLoanDeduction())
                .advanceDeduction(salary.getAdvanceDeduction())
                .otherDeduction(salary.getOtherDeduction())
                .netSalary(salary.getNetSalary())
                .paymentStatus(salary.getPaymentStatus())
                .paymentDate(salary.getPaymentDate())
                .paymentMethod(paymentMethodDTO)
                .ledgerTransactionId(salary.getLedgerTransaction() != null ? salary.getLedgerTransaction().getId() : null)
                .ledgerTransactionCode(salary.getLedgerTransaction() != null
                        ? ledgerService.findTransaction(salary.getLedgerTransaction().getId()).getTransactionCode()
                        : null)
                .remarks(salary.getRemarks())
                .build();
    }
}
