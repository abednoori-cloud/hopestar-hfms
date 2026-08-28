package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.module.finance.ledger.dto.CurrencyResponseDTO;

import java.util.List;

/**
 * Read-only access to the {@code currencies} master list. Currencies
 * (USD, AFN, EUR) are seeded once via Flyway (V5); this system has no
 * screen for adding new currencies, since doing so would require an
 * accompanying code change anywhere {@link
 * com.hopestar.hfms.common.enums.SupportedCurrency} is used.
 */
public interface CurrencyService {

    List<CurrencyResponseDTO> listActive();

    CurrencyResponseDTO getByCode(String code);

    CurrencyResponseDTO getBaseCurrency();
}
