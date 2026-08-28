package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.ledger.dto.CurrencyResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import com.hopestar.hfms.module.finance.ledger.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Override
    public List<CurrencyResponseDTO> listActive() {
        return currencyRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public CurrencyResponseDTO getByCode(String code) {
        return currencyRepository.findByCodeAndActiveTrue(code)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", code));
    }

    @Override
    public CurrencyResponseDTO getBaseCurrency() {
        return currencyRepository.findFirstByBaseCurrencyTrueAndActiveTrue()
                .map(this::toResponseDTO)
                .orElseThrow(() -> new BusinessValidationException("No base currency is configured."));
    }

    private CurrencyResponseDTO toResponseDTO(Currency currency) {
        return CurrencyResponseDTO.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .baseCurrency(currency.isBaseCurrency())
                .build();
    }
}
