package com.hopestar.hfms.module.finance.expense.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseCreateDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseResponseDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseSearchDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseUpdateDTO;
import com.hopestar.hfms.module.finance.expense.entity.Expense;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseCategory;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import com.hopestar.hfms.module.finance.expense.repository.ExpenseCategoryRepository;
import com.hopestar.hfms.module.finance.expense.repository.ExpenseRepository;
import com.hopestar.hfms.module.finance.expense.repository.ExpenseSpecifications;
import com.hopestar.hfms.module.finance.ledger.dto.PostTransactionRequest;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionType;
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
 * Implements {@link ExpenseService}. See that interface's Javadoc for the
 * governing business rules. Mirrors {@code EmployeeAdvanceServiceImpl}'s
 * shape, minus the repayment concept an expense doesn't have.
 * <p>
 * Only {@code EXPENSE_VIEW} and {@code EXPENSE_APPROVE} were pre-seeded
 * (no {@code EXPENSE_MANAGE}); per the approved simple DRAFT -&gt;
 * POSTED/VOID pattern (no separate approval step), every write operation
 * here -- create, update, post, void -- requires {@code EXPENSE_APPROVE},
 * since posting an expense is functionally the approval. Read methods
 * carry no method-level check, matching {@code EmployeeLoanServiceImpl}/
 * {@code EmployeeAdvanceServiceImpl}'s reads, which rely on the general
 * "any authenticated user" rule enforced by {@code SecurityConfig} rather
 * than a per-permission check.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private static final String EXPENSE_SEQUENCE_KEY = "EXPENSE";
    private static final String EXPENSE_CODE_PREFIX = "EXP";
    private static final int EXPENSE_CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ExpenseResponseDTO create(ExpenseCreateDTO createDTO) {
        ExpenseCategory category = resolveCategory(createDTO.getCategoryId());

        BigDecimal amount = MoneyUtil.normalize(createDTO.getAmount());

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(amount, exchangeRate);

        String expenseNumber = sequenceGeneratorService.nextYearlyValue(
                EXPENSE_SEQUENCE_KEY, EXPENSE_CODE_PREFIX, EXPENSE_CODE_PADDING, null);

        Expense expense = Expense.builder()
                .expenseNumber(expenseNumber)
                .category(category)
                .description(createDTO.getDescription())
                .amount(amount)
                .currency(createDTO.getCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .expenseDate(createDTO.getExpenseDate())
                .status(ExpenseStatus.DRAFT)
                .remarks(createDTO.getRemarks())
                .build();

        Expense saved = expenseRepository.save(expense);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ExpenseResponseDTO update(Long expenseId, ExpenseUpdateDTO updateDTO) {
        Expense expense = getActiveExpenseEntity(expenseId);
        requireDraft(expense);

        ExpenseCategory category = resolveCategory(updateDTO.getCategoryId());

        BigDecimal amount = MoneyUtil.normalize(updateDTO.getAmount());

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(updateDTO.getCurrency(), updateDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(amount, exchangeRate);

        expense.setCategory(category);
        expense.setDescription(updateDTO.getDescription());
        expense.setAmount(amount);
        expense.setCurrency(updateDTO.getCurrency());
        expense.setExchangeRateToUsd(exchangeRate);
        expense.setUsdEquivalentAmount(usdEquivalentAmount);
        expense.setExpenseDate(updateDTO.getExpenseDate());
        expense.setRemarks(updateDTO.getRemarks());

        return toResponseDTO(expense);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ExpenseResponseDTO post(Long expenseId, Long paymentMethodId) {
        Expense expense = getActiveExpenseEntity(expenseId);

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Expense " + expense.getExpenseNumber() + " can only be posted while DRAFT (currently "
                            + expense.getStatus() + ").");
        }

        PostTransactionRequest request = PostTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .originalAmount(expense.getAmount())
                .currencyCode(expense.getCurrency())
                .exchangeRateToUsd(expense.getExchangeRateToUsd())
                .paymentMethodId(paymentMethodId)
                .transactionDate(expense.getExpenseDate())
                .referenceTable("expenses")
                .referenceId(expense.getId())
                .notes(expense.getCategory().getName() + " expense " + expense.getExpenseNumber()
                        + ": " + expense.getDescription())
                .build();

        // LedgerService remains the only creator of Transaction rows --
        // this method never constructs one itself.
        TransactionResponseDTO postedTransaction = ledgerService.postExpense(request);
        Transaction transactionReference = transactionRepository.getReferenceById(postedTransaction.getId());

        expense.setLedgerTransaction(transactionReference);
        expense.setStatus(ExpenseStatus.POSTED);

        return toResponseDTO(expense);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ExpenseResponseDTO voidExpense(Long expenseId, String reason) {
        Expense expense = getActiveExpenseEntity(expenseId);

        if (expense.getStatus() == ExpenseStatus.VOID) {
            throw new BusinessValidationException("Expense " + expense.getExpenseNumber() + " is already VOID.");
        }

        if (expense.getStatus() == ExpenseStatus.POSTED && expense.getLedgerTransaction() != null) {
            // Voiding, never mutating, the ledger row directly -- LedgerService
            // remains the only class that changes a Transaction's status.
            ledgerService.voidTransaction(expense.getLedgerTransaction().getId(), reason);
        }

        expense.setStatus(ExpenseStatus.VOID);
        return toResponseDTO(expense);
    }

    @Override
    public ExpenseResponseDTO getById(Long expenseId) {
        return toResponseDTO(getActiveExpenseEntity(expenseId));
    }

    @Override
    public ExpenseResponseDTO getByExpenseNumber(String expenseNumber) {
        Expense expense = expenseRepository.findByExpenseNumberAndActiveTrue(expenseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseNumber));
        return toResponseDTO(expense);
    }

    @Override
    public PageResponse<ExpenseResponseDTO> search(ExpenseSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        String sortField = ExpenseSpecifications.resolveSortField(searchDTO.getSortBy());
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDTO.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<Expense> result = expenseRepository.findAll(
                ExpenseSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(direction, sortField)));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    public List<ExpenseResponseDTO> listAll() {
        return expenseRepository.findByActiveTrueOrderByExpenseDateDesc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Expense getActiveExpenseEntity(Long expenseId) {
        return expenseRepository.findByIdAndActiveTrue(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
    }

    private void requireDraft(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Expense " + expense.getExpenseNumber() + " can no longer be edited because it is "
                            + expense.getStatus() + ", not DRAFT.");
        }
    }

    private ExpenseCategory resolveCategory(Long categoryId) {
        return expenseCategoryRepository.findById(categoryId)
                .filter(ExpenseCategory::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category", categoryId));
    }

    private ExpenseResponseDTO toResponseDTO(Expense expense) {
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .active(expense.isActive())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .createdBy(expense.getCreatedBy())
                .updatedBy(expense.getUpdatedBy())
                .expenseNumber(expense.getExpenseNumber())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .exchangeRateToUsd(expense.getExchangeRateToUsd())
                .usdEquivalentAmount(expense.getUsdEquivalentAmount())
                .expenseDate(expense.getExpenseDate())
                .status(expense.getStatus())
                .remarks(expense.getRemarks())
                .ledgerTransactionId(expense.getLedgerTransaction() != null ? expense.getLedgerTransaction().getId() : null)
                .ledgerTransactionCode(expense.getLedgerTransaction() != null
                        ? ledgerService.findTransaction(expense.getLedgerTransaction().getId()).getTransactionCode()
                        : null)
                .build();
    }
}
