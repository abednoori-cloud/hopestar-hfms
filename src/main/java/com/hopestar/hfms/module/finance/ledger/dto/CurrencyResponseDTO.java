package com.hopestar.hfms.module.finance.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String symbol;
    private boolean baseCurrency;
}
