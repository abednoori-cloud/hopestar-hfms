package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.module.finance.ledger.dto.CurrencyResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import lombok.experimental.UtilityClass;

/**
 * Single source of truth for mapping a {@link Transaction} entity to its
 * read-model DTO. Both {@link LedgerServiceImpl} (which returns a DTO
 * immediately after posting/voiding) and {@link TransactionServiceImpl}
 * (which returns DTOs from search/lookup) call this instead of each
 * duplicating the same mapping logic.
 */
@UtilityClass
class TransactionMapper {

    TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .active(transaction.isActive())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .createdBy(transaction.getCreatedBy())
                .updatedBy(transaction.getUpdatedBy())
                .transactionCode(transaction.getTransactionCode())
                .transactionType(transaction.getTransactionType())
                .direction(transaction.getDirection())
                .originalAmount(transaction.getOriginalAmount())
                .currency(toCurrencyDTO(transaction.getCurrency()))
                .exchangeRateToUsd(transaction.getExchangeRateToUsd())
                .usdEquivalentAmount(transaction.getUsdEquivalentAmount())
                .paymentMethod(toPaymentMethodDTO(transaction.getPaymentMethod()))
                .transactionDate(transaction.getTransactionDate())
                .referenceTable(transaction.getReferenceTable())
                .referenceId(transaction.getReferenceId())
                .status(transaction.getStatus())
                .notes(transaction.getNotes())
                .branchName(transaction.getBranch() != null ? transaction.getBranch().getName() : null)
                .build();
    }

    private CurrencyResponseDTO toCurrencyDTO(Currency currency) {
        return CurrencyResponseDTO.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .baseCurrency(currency.isBaseCurrency())
                .build();
    }

    private PaymentMethodResponseDTO toPaymentMethodDTO(PaymentMethod paymentMethod) {
        return PaymentMethodResponseDTO.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .code(paymentMethod.getCode())
                .description(paymentMethod.getDescription())
                .build();
    }
}
