package com.hopestar.hfms.module.finance.advance.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.finance.advance.dto.AdvanceRepaymentCreateDTO;
import com.hopestar.hfms.module.finance.advance.dto.AdvanceRepaymentResponseDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceCreateDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceResponseDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceSearchDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceUpdateDTO;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceRepaymentSchedule;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import com.hopestar.hfms.module.finance.advance.entity.EmployeeAdvance;
import com.hopestar.hfms.module.finance.advance.repository.AdvanceRepaymentScheduleRepository;
import com.hopestar.hfms.module.finance.advance.repository.EmployeeAdvanceRepository;
import com.hopestar.hfms.module.finance.advance.repository.EmployeeAdvanceSpecifications;
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
 * Implements {@link EmployeeAdvanceService}. See that interface's Javadoc
 * for the governing business rules. Mirrors {@code EmployeeLoanServiceImpl}
 * exactly, minus the fixed monthly-deduction repayment schedule a formal
 * loan has.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeAdvanceServiceImpl implements EmployeeAdvanceService {

    private static final String ADVANCE_SEQUENCE_KEY = "ADVANCE";
    private static final String ADVANCE_CODE_PREFIX = "ADV";
    private static final int ADVANCE_CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeAdvanceRepository employeeAdvanceRepository;
    private final AdvanceRepaymentScheduleRepository advanceRepaymentScheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADVANCE_MANAGE')")
    public EmployeeAdvanceResponseDTO create(EmployeeAdvanceCreateDTO createDTO) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(createDTO.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", createDTO.getEmployeeId()));

        BigDecimal advanceAmount = MoneyUtil.normalize(createDTO.getAdvanceAmount());

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(advanceAmount, exchangeRate);

        String advanceNumber = sequenceGeneratorService.nextYearlyValue(
                ADVANCE_SEQUENCE_KEY, ADVANCE_CODE_PREFIX, ADVANCE_CODE_PADDING, null);

        EmployeeAdvance advance = EmployeeAdvance.builder()
                .advanceNumber(advanceNumber)
                .employee(employee)
                .advanceAmount(advanceAmount)
                .currency(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .remainingBalance(advanceAmount)
                .advanceDate(createDTO.getAdvanceDate())
                .status(AdvanceStatus.DRAFT)
                .remarks(createDTO.getRemarks())
                .build();

        EmployeeAdvance saved = employeeAdvanceRepository.save(advance);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADVANCE_MANAGE')")
    public EmployeeAdvanceResponseDTO update(Long advanceId, EmployeeAdvanceUpdateDTO updateDTO) {
        EmployeeAdvance advance = getActiveAdvanceEntity(advanceId);
        requireDraft(advance);

        BigDecimal advanceAmount = MoneyUtil.normalize(updateDTO.getAdvanceAmount());

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(updateDTO.getCurrency(), updateDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(advanceAmount, exchangeRate);

        advance.setAdvanceAmount(advanceAmount);
        advance.setCurrency(updateDTO.getCurrency());
        advance.setExchangeRateToUsd(exchangeRate);
        advance.setUsdEquivalentAmount(usdEquivalentAmount);
        // Safe to reset outright: a DRAFT advance can never have had a
        // repayment posted against it (repayments require ACTIVE).
        advance.setRemainingBalance(advanceAmount);
        advance.setAdvanceDate(updateDTO.getAdvanceDate());
        advance.setRemarks(updateDTO.getRemarks());

        return toResponseDTO(advance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADVANCE_MANAGE')")
    public EmployeeAdvanceResponseDTO disburse(Long advanceId, Long paymentMethodId) {
        EmployeeAdvance advance = getActiveAdvanceEntity(advanceId);

        if (advance.getStatus() != AdvanceStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Advance " + advance.getAdvanceNumber() + " can only be disbursed while DRAFT (currently "
                            + advance.getStatus() + ").");
        }

        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.ADVANCE)
                .originalAmount(advance.getAdvanceAmount())
                .currencyCode(advance.getCurrency())
                .exchangeRateToUsd(advance.getExchangeRateToUsd())
                .paymentMethodId(paymentMethodId)
                .transactionDate(advance.getAdvanceDate())
                .referenceTable("employee_advances")
                .referenceId(advance.getId())
                .branchId(advance.getEmployee().getBranch() != null ? advance.getEmployee().getBranch().getId() : null)
                .notes("Disbursement of advance " + advance.getAdvanceNumber() + " to "
                        + advance.getEmployee().getEmployeeCode())
                .build();

        // LedgerService remains the only creator of Transaction rows --
        // this method never constructs one itself.
        TransactionResponseDTO postedTransaction = ledgerService.postExpense(request);
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());

        advance.setLedgerTransaction(transactionReference);
        advance.setStatus(AdvanceStatus.ACTIVE);

        return toResponseDTO(advance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADVANCE_MANAGE')")
    public EmployeeAdvanceResponseDTO voidAdvance(Long advanceId, String reason) {
        EmployeeAdvance advance = getActiveAdvanceEntity(advanceId);

        if (advance.getStatus() == AdvanceStatus.VOID) {
            throw new BusinessValidationException("Advance " + advance.getAdvanceNumber() + " is already VOID.");
        }
        if (advance.getStatus() == AdvanceStatus.CLOSED) {
            throw new BusinessValidationException(
                    "Advance " + advance.getAdvanceNumber() + " is fully repaid and can no longer be voided.");
        }

        if (advance.getStatus() == AdvanceStatus.ACTIVE && advance.getLedgerTransaction() != null) {
            // Voiding, never mutating, the ledger row directly -- LedgerService
            // remains the only class that changes a Transaction's status.
            ledgerService.voidTransaction(advance.getLedgerTransaction().getId(), reason);
        }

        advance.setStatus(AdvanceStatus.VOID);
        return toResponseDTO(advance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADVANCE_MANAGE')")
    public AdvanceRepaymentResponseDTO recordRepayment(Long advanceId, AdvanceRepaymentCreateDTO createDTO) {
        EmployeeAdvance advance = getActiveAdvanceEntity(advanceId);

        if (advance.getStatus() != AdvanceStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Advance " + advance.getAdvanceNumber() + " does not accept repayments while " + advance.getStatus()
                            + " (must be ACTIVE).");
        }

        BigDecimal amount = MoneyUtil.normalize(createDTO.getAmount());
        if (MoneyUtil.isNegative(MoneyUtil.subtract(advance.getRemainingBalance(), amount))) {
            throw new BusinessValidationException(
                    "Repayment amount " + amount + " exceeds the remaining balance "
                            + advance.getRemainingBalance() + " on advance " + advance.getAdvanceNumber() + ".");
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(createDTO.getPaymentMethodId());

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(amount, exchangeRate);

        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.ADVANCE_REPAYMENT)
                .originalAmount(amount)
                .currencyCode(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .paymentMethodId(createDTO.getPaymentMethodId())
                .transactionDate(createDTO.getRepaymentDate())
                .referenceTable("advance_repayments")
                .referenceId(advance.getId())
                .branchId(advance.getEmployee().getBranch() != null ? advance.getEmployee().getBranch().getId() : null)
                .notes("Repayment against advance " + advance.getAdvanceNumber() + " by "
                        + advance.getEmployee().getEmployeeCode())
                .build();

        // LedgerService remains the only creator of Transaction rows --
        // this method never constructs one itself.
        TransactionResponseDTO postedTransaction = ledgerService.postIncome(request);
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());

        AdvanceRepaymentSchedule repayment = AdvanceRepaymentSchedule.builder()
                .advance(advance)
                .repaymentDate(createDTO.getRepaymentDate())
                .amount(amount)
                .currency(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .paymentMethod(paymentMethod)
                .ledgerTransaction(transactionReference)
                .notes(createDTO.getNotes())
                .build();
        AdvanceRepaymentSchedule savedRepayment = advanceRepaymentScheduleRepository.save(repayment);

        BigDecimal newBalance = MoneyUtil.subtract(advance.getRemainingBalance(), amount);
        advance.setRemainingBalance(newBalance);
        if (MoneyUtil.isZero(newBalance)) {
            advance.setStatus(AdvanceStatus.CLOSED);
        }

        return toRepaymentResponseDTO(savedRepayment);
    }

    @Override
    public EmployeeAdvanceResponseDTO getById(Long advanceId) {
        return toResponseDTO(getActiveAdvanceEntity(advanceId));
    }

    @Override
    public EmployeeAdvanceResponseDTO getByAdvanceNumber(String advanceNumber) {
        EmployeeAdvance advance = employeeAdvanceRepository.findByAdvanceNumberAndActiveTrue(advanceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Advance", advanceNumber));
        return toResponseDTO(advance);
    }

    @Override
    public PageResponse<EmployeeAdvanceResponseDTO> search(EmployeeAdvanceSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        String sortField = EmployeeAdvanceSpecifications.resolveSortField(searchDTO.getSortBy());
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDTO.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<EmployeeAdvance> result = employeeAdvanceRepository.findAll(
                EmployeeAdvanceSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(direction, sortField)));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    public List<EmployeeAdvanceResponseDTO> listByEmployee(Long employeeId) {
        return employeeAdvanceRepository.findByEmployeeIdAndActiveTrueOrderByAdvanceDateDesc(employeeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<AdvanceRepaymentResponseDTO> listRepayments(Long advanceId) {
        return advanceRepaymentScheduleRepository.findByAdvanceIdAndActiveTrueOrderByRepaymentDateDesc(advanceId).stream()
                .map(this::toRepaymentResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private EmployeeAdvance getActiveAdvanceEntity(Long advanceId) {
        return employeeAdvanceRepository.findByIdAndActiveTrue(advanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Advance", advanceId));
    }

    private void requireDraft(EmployeeAdvance advance) {
        if (advance.getStatus() != AdvanceStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Advance " + advance.getAdvanceNumber() + " can no longer be edited because it is "
                            + advance.getStatus() + ", not DRAFT.");
        }
    }

    private PaymentMethod resolvePaymentMethod(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method", paymentMethodId));
    }

    private EmployeeAdvanceResponseDTO toResponseDTO(EmployeeAdvance advance) {
        Employee employee = advance.getEmployee();

        return EmployeeAdvanceResponseDTO.builder()
                .id(advance.getId())
                .active(advance.isActive())
                .createdAt(advance.getCreatedAt())
                .updatedAt(advance.getUpdatedAt())
                .createdBy(advance.getCreatedBy())
                .updatedBy(advance.getUpdatedBy())
                .advanceNumber(advance.getAdvanceNumber())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeFullName(employee.getFullName())
                .advanceAmount(advance.getAdvanceAmount())
                .currency(advance.getCurrency())
                .exchangeRateToUsd(advance.getExchangeRateToUsd())
                .usdEquivalentAmount(advance.getUsdEquivalentAmount())
                .remainingBalance(advance.getRemainingBalance())
                .advanceDate(advance.getAdvanceDate())
                .status(advance.getStatus())
                .remarks(advance.getRemarks())
                .ledgerTransactionId(advance.getLedgerTransaction() != null ? advance.getLedgerTransaction().getId() : null)
                .ledgerTransactionCode(advance.getLedgerTransaction() != null
                        ? ledgerService.findTransaction(advance.getLedgerTransaction().getId()).getTransactionCode()
                        : null)
                .build();
    }

    private AdvanceRepaymentResponseDTO toRepaymentResponseDTO(AdvanceRepaymentSchedule repayment) {
        PaymentMethodResponseDTO paymentMethodDTO = PaymentMethodResponseDTO.builder()
                .id(repayment.getPaymentMethod().getId())
                .name(repayment.getPaymentMethod().getName())
                .code(repayment.getPaymentMethod().getCode())
                .description(repayment.getPaymentMethod().getDescription())
                .build();

        return AdvanceRepaymentResponseDTO.builder()
                .advanceId(repayment.getAdvance().getId())
                .advanceNumber(repayment.getAdvance().getAdvanceNumber())
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
