package com.hopestar.hfms.module.finance.studentpayment.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.finance.ledger.dto.CurrencyResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.PostTransactionRequest;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
import com.hopestar.hfms.module.finance.ledger.repository.CurrencyRepository;
import com.hopestar.hfms.module.finance.ledger.repository.PaymentMethodRepository;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import com.hopestar.hfms.module.finance.ledger.service.LedgerService;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentCreateDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentResponseDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentSearchDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentUpdateDTO;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.entity.StudentPayment;
import com.hopestar.hfms.module.finance.studentpayment.repository.StudentPaymentRepository;
import com.hopestar.hfms.module.finance.studentpayment.repository.StudentPaymentSpecifications;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import com.hopestar.hfms.module.student.entity.Student;
import com.hopestar.hfms.module.student.entity.StudentContract;
import com.hopestar.hfms.module.student.repository.StudentContractRepository;
import com.hopestar.hfms.module.student.repository.StudentRepository;
import com.hopestar.hfms.module.student.service.StudentContractService;
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
 * Implements {@link StudentPaymentService}. See that interface's Javadoc
 * for the governing business rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPaymentServiceImpl implements StudentPaymentService {

    private static final String PAYMENT_SEQUENCE_KEY = "STUDENT_PAYMENT";
    private static final String PAYMENT_CODE_PREFIX = "PAY";
    private static final String RECEIPT_SEQUENCE_KEY = "STUDENT_PAYMENT_RECEIPT";
    private static final String RECEIPT_CODE_PREFIX = "RCPT";
    private static final int CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final StudentPaymentRepository studentPaymentRepository;
    private final StudentRepository studentRepository;
    private final StudentContractRepository studentContractRepository;
    private final StudentContractService studentContractService;
    private final CurrencyRepository currencyRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public StudentPaymentResponseDTO create(StudentPaymentCreateDTO createDTO) {
        Student student = studentRepository.findByIdAndActiveTrue(createDTO.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", createDTO.getStudentId()));

        StudentContract contract = studentContractRepository.findByIdAndActiveTrue(createDTO.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Student contract", createDTO.getContractId()));

        if (!contract.getStudent().getId().equals(student.getId())) {
            throw new BusinessValidationException(
                    "Contract " + contract.getId() + " does not belong to student " + student.getStudentCode() + ".");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Payments can only be recorded against an ACTIVE contract; contract "
                            + contract.getId() + " is " + contract.getStatus() + ".");
        }

        BigDecimal originalAmount = MoneyUtil.normalize(createDTO.getOriginalAmount());
        if (!MoneyUtil.isPositive(originalAmount)) {
            throw new BusinessValidationException("Payment amount must be positive.");
        }

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrencyCode(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(originalAmount, exchangeRate);

        Currency currency = resolveCurrency(createDTO.getCurrencyCode());
        PaymentMethod paymentMethod = resolvePaymentMethod(createDTO.getPaymentMethodId());

        boolean saveAsDraft = createDTO.isSaveAsDraft();
        if (!saveAsDraft) {
            validateNotOverpaying(contract.getId(), usdEquivalentAmount);
        }

        Long branchId = student.getBranch() != null ? student.getBranch().getId() : null;
        String paymentNumber = sequenceGeneratorService.nextYearlyValue(
                PAYMENT_SEQUENCE_KEY, PAYMENT_CODE_PREFIX, CODE_PADDING, branchId);
        String receiptNumber = sequenceGeneratorService.nextYearlyValue(
                RECEIPT_SEQUENCE_KEY, RECEIPT_CODE_PREFIX, CODE_PADDING, branchId);

        StudentPayment payment = StudentPayment.builder()
                .paymentNumber(paymentNumber)
                .receiptNumber(receiptNumber)
                .invoiceId(createDTO.getInvoiceId())
                .student(student)
                .contract(contract)
                .paymentDate(createDTO.getPaymentDate())
                .originalAmount(originalAmount)
                .currency(currency)
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .paymentMethod(paymentMethod)
                .referenceNumber(createDTO.getReferenceNumber())
                .notes(createDTO.getNotes())
                .status(saveAsDraft ? PaymentStatus.DRAFT : PaymentStatus.POSTED)
                .build();

        StudentPayment saved = studentPaymentRepository.save(payment);

        if (!saveAsDraft) {
            postToLedger(saved);
        }

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public StudentPaymentResponseDTO update(Long paymentId, StudentPaymentUpdateDTO updateDTO) {
        StudentPayment payment = getActivePaymentEntity(paymentId);
        requireDraft(payment);

        BigDecimal originalAmount = MoneyUtil.normalize(updateDTO.getOriginalAmount());
        if (!MoneyUtil.isPositive(originalAmount)) {
            throw new BusinessValidationException("Payment amount must be positive.");
        }

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(updateDTO.getCurrencyCode(), updateDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(originalAmount, exchangeRate);

        Currency currency = resolveCurrency(updateDTO.getCurrencyCode());
        PaymentMethod paymentMethod = resolvePaymentMethod(updateDTO.getPaymentMethodId());

        payment.setPaymentDate(updateDTO.getPaymentDate());
        payment.setOriginalAmount(originalAmount);
        payment.setCurrency(currency);
        payment.setExchangeRateToUsd(exchangeRate);
        payment.setUsdEquivalentAmount(usdEquivalentAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setReferenceNumber(updateDTO.getReferenceNumber());
        payment.setNotes(updateDTO.getNotes());

        return toResponseDTO(payment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public StudentPaymentResponseDTO post(Long paymentId) {
        StudentPayment payment = getActivePaymentEntity(paymentId);
        requireDraft(payment);

        if (payment.getContract().getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Payments can only be posted against an ACTIVE contract; contract "
                            + payment.getContract().getId() + " is " + payment.getContract().getStatus() + ".");
        }

        validateNotOverpaying(payment.getContract().getId(), payment.getUsdEquivalentAmount());

        payment.setStatus(PaymentStatus.POSTED);
        postToLedger(payment);

        return toResponseDTO(payment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public StudentPaymentResponseDTO cancel(Long paymentId, String reason) {
        StudentPayment payment = getActivePaymentEntity(paymentId);

        if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessValidationException(
                    "Payment " + payment.getPaymentNumber() + " is already " + payment.getStatus() + ".");
        }

        if (payment.getStatus() == PaymentStatus.POSTED && payment.getTransaction() != null) {
            // Voiding, never mutating, the ledger row directly -- LedgerService
            // remains the only class that changes a Transaction's status.
            ledgerService.voidTransaction(payment.getTransaction().getId(), reason);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        return toResponseDTO(payment);
    }

    @Override
    public StudentPaymentResponseDTO getById(Long paymentId) {
        return toResponseDTO(getActivePaymentEntity(paymentId));
    }

    @Override
    public StudentPaymentResponseDTO getByPaymentNumber(String paymentNumber) {
        StudentPayment payment = studentPaymentRepository.findByPaymentNumberAndActiveTrue(paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student payment", paymentNumber));
        return toResponseDTO(payment);
    }

    @Override
    public PageResponse<StudentPaymentResponseDTO> search(StudentPaymentSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        Page<StudentPayment> result = studentPaymentRepository.findAll(
                StudentPaymentSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentDate")));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    public List<StudentPaymentResponseDTO> listByStudent(Long studentId) {
        return studentPaymentRepository.findByStudentIdAndActiveTrueOrderByPaymentDateDesc(studentId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<StudentPaymentResponseDTO> listByContract(Long contractId) {
        return studentPaymentRepository.findByContractIdAndActiveTrueOrderByPaymentDateDesc(contractId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private StudentPayment getActivePaymentEntity(Long paymentId) {
        return studentPaymentRepository.findByIdAndActiveTrue(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student payment", paymentId));
    }

    private void requireDraft(StudentPayment payment) {
        if (payment.getStatus() != PaymentStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Payment " + payment.getPaymentNumber() + " can no longer be edited because it is "
                            + payment.getStatus() + ", not DRAFT.");
        }
    }

    /**
     * Enforces the overpayment rule: {@code payment.usdEquivalentAmount}
     * must not exceed the contract's current remaining balance. Reuses
     * {@code StudentContractService.getRemainingBalanceUsd} — the single
     * place that figure is computed — rather than re-deriving it here.
     */
    private void validateNotOverpaying(Long contractId, BigDecimal paymentUsdEquivalent) {
        BigDecimal remainingBalance = studentContractService.getRemainingBalanceUsd(contractId);
        if (paymentUsdEquivalent.compareTo(remainingBalance) > 0) {
            throw new BusinessValidationException(
                    "Payment of USD %.2f exceeds the contract's remaining balance of USD %.2f."
                            .formatted(paymentUsdEquivalent, remainingBalance));
        }
    }

    /**
     * Posts the payment's already-computed, already-validated amount to
     * the ledger via {@code LedgerService.postIncome} and links the
     * resulting transaction back onto this payment. This is the only
     * place in the Student Payments module that calls {@code
     * LedgerService} — no other method constructs a {@code
     * PostTransactionRequest}, and this method never constructs a {@code
     * Transaction} itself; it only attaches a reference to the one
     * {@code LedgerService} already created.
     */
    private void postToLedger(StudentPayment payment) {
        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.STUDENT_PAYMENT)
                .originalAmount(payment.getOriginalAmount())
                .currencyCode(SupportedCurrency.valueOf(payment.getCurrency().getCode()))
                .exchangeRateToUsd(payment.getExchangeRateToUsd())
                .paymentMethodId(payment.getPaymentMethod().getId())
                .transactionDate(payment.getPaymentDate())
                .referenceTable("student_payments")
                .referenceId(payment.getId())
                .branchId(payment.getStudent().getBranch() != null ? payment.getStudent().getBranch().getId() : null)
                .notes("Student payment " + payment.getPaymentNumber() + " (receipt " + payment.getReceiptNumber() + ")")
                .build();

        TransactionResponseDTO postedTransaction = ledgerService.postIncome(request);

        // getReferenceById obtains a lazy JPA proxy for the already-
        // committed Transaction without a second round trip -- exactly
        // enough for Hibernate to persist the foreign key on this managed
        // StudentPayment. It never fetches or mutates the Transaction's
        // own state, keeping LedgerService the sole writer of that entity.
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());
        payment.setTransaction(transactionReference);
    }

    private Currency resolveCurrency(SupportedCurrency currencyCode) {
        return currencyRepository.findByCodeAndActiveTrue(currencyCode.name())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", currencyCode));
    }

    private PaymentMethod resolvePaymentMethod(Long paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method", paymentMethodId));
    }

    private StudentPaymentResponseDTO toResponseDTO(StudentPayment payment) {
        Student student = payment.getStudent();
        StudentContract contract = payment.getContract();

        return StudentPaymentResponseDTO.builder()
                .id(payment.getId())
                .active(payment.isActive())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .createdBy(payment.getCreatedBy())
                .updatedBy(payment.getUpdatedBy())
                .paymentNumber(payment.getPaymentNumber())
                .receiptNumber(payment.getReceiptNumber())
                .invoiceId(payment.getInvoiceId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentFullName(student.getFullName())
                .contractId(contract.getId())
                .contractRemainingBalance(studentContractService.getRemainingBalanceUsd(contract.getId()))
                .transactionId(payment.getTransaction() != null ? payment.getTransaction().getId() : null)
                .transactionCode(payment.getTransaction() != null
                        ? ledgerService.findTransaction(payment.getTransaction().getId()).getTransactionCode()
                        : null)
                .paymentDate(payment.getPaymentDate())
                .originalAmount(payment.getOriginalAmount())
                .currency(CurrencyResponseDTO.builder()
                        .id(payment.getCurrency().getId())
                        .code(payment.getCurrency().getCode())
                        .name(payment.getCurrency().getName())
                        .symbol(payment.getCurrency().getSymbol())
                        .baseCurrency(payment.getCurrency().isBaseCurrency())
                        .build())
                .exchangeRateToUsd(payment.getExchangeRateToUsd())
                .usdEquivalentAmount(payment.getUsdEquivalentAmount())
                .paymentMethod(PaymentMethodResponseDTO.builder()
                        .id(payment.getPaymentMethod().getId())
                        .name(payment.getPaymentMethod().getName())
                        .code(payment.getPaymentMethod().getCode())
                        .description(payment.getPaymentMethod().getDescription())
                        .build())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .status(payment.getStatus())
                .build();
    }
}
