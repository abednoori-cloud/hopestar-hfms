package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.auth.entity.Branch;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import com.hopestar.hfms.module.finance.ledger.dto.PostTransactionRequest;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.repository.CurrencyRepository;
import com.hopestar.hfms.module.finance.ledger.repository.PaymentMethodRepository;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Implements {@link LedgerService} -- the only class in the system
 * permitted to create or void a {@link Transaction}. See the interface
 * Javadoc for the architectural rationale.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerServiceImpl implements LedgerService {

    private static final String TRANSACTION_SEQUENCE_KEY = "TRANSACTION";
    private static final String TRANSACTION_CODE_PREFIX = "TXN";
    private static final int TRANSACTION_CODE_PADDING = 6;

    private final TransactionRepository transactionRepository;
    private final CurrencyRepository currencyRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final BranchRepository branchRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    @Transactional
    public TransactionResponseDTO postIncome(PostTransactionRequest request) {
        return post(request, Direction.INCOME);
    }

    @Override
    @Transactional
    public TransactionResponseDTO postExpense(PostTransactionRequest request) {
        return post(request, Direction.EXPENSE);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('TRANSACTION_VOID')")
    public TransactionResponseDTO voidTransaction(Long transactionId, String reason) {
        Transaction transaction = getActiveTransactionEntity(transactionId);
        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessValidationException(
                    "Transaction " + transaction.getTransactionCode() + " has already been voided.");
        }
        transaction.setStatus(TransactionStatus.VOIDED);
        if (StringUtils.hasText(reason)) {
            String existingNotes = transaction.getNotes();
            String voidNote = "[VOIDED] " + reason;
            transaction.setNotes(StringUtils.hasText(existingNotes) ? existingNotes + " | " + voidNote : voidNote);
        }
        return TransactionMapper.toResponseDTO(transaction);
    }

    @Override
    public TransactionResponseDTO findTransaction(Long transactionId) {
        return TransactionMapper.toResponseDTO(getActiveTransactionEntity(transactionId));
    }

    @Override
    public TransactionResponseDTO findTransactionByCode(String transactionCode) {
        Transaction transaction = transactionRepository.findByTransactionCodeAndActiveTrue(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionCode));
        return TransactionMapper.toResponseDTO(transaction);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private TransactionResponseDTO post(PostTransactionRequest request, Direction direction) {
        Currency currency = resolveCurrency(request.getCurrencyCode());
        PaymentMethod paymentMethod = resolvePaymentMethod(request.getPaymentMethodId());
        Branch branch = resolveBranch(request.getBranchId());

        BigDecimal originalAmount = MoneyUtil.normalize(request.getOriginalAmount());
        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(request.getCurrencyCode(), request.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(originalAmount, exchangeRate);

        String transactionCode = sequenceGeneratorService.nextYearlyValue(
                TRANSACTION_SEQUENCE_KEY, TRANSACTION_CODE_PREFIX, TRANSACTION_CODE_PADDING, request.getBranchId());

        Transaction transaction = Transaction.builder()
                .transactionCode(transactionCode)
                .transactionType(request.getTransactionType())
                .direction(direction)
                .originalAmount(originalAmount)
                .currency(currency)
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .paymentMethod(paymentMethod)
                .transactionDate(request.getTransactionDate())
                .referenceTable(request.getReferenceTable())
                .referenceId(request.getReferenceId())
                .status(TransactionStatus.POSTED)
                .notes(request.getNotes())
                .branch(branch)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return TransactionMapper.toResponseDTO(saved);
    }

    private Transaction getActiveTransactionEntity(Long transactionId) {
        return transactionRepository.findByIdAndActiveTrue(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
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

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) {
            return branchRepository.findFirstByHeadquartersTrueAndActiveTrue().orElse(null);
        }
        return branchRepository.findById(branchId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }
}
