package com.hopestar.hfms.module.student.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramResponseDTO {

    private Long id;
    private String name;
    private String destinationCountry;
    private BigDecimal basePrice;
    private SupportedCurrency currencyCode;
    private String description;
    private boolean active;
}
