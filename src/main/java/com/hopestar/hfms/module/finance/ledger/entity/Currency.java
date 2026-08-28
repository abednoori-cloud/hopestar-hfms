package com.hopestar.hfms.module.finance.ledger.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A supported currency (USD, AFN, EUR). Exactly one row has
 * {@link #baseCurrency} {@code = true} (USD) — every {@code
 * exchangeRateToUsd} recorded on a {@link Transaction} is quoted against
 * that base currency. Rates themselves are entered manually per
 * transaction (this system has no live FX feed); this table is a
 * reference list of which currencies are accepted, not a rate table.
 */
@Getter
@Setter
@Entity
@Table(name = "currencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_currencies_code", columnNames = "code")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Currency extends BaseEntity {

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Size(max = 5)
    @Column(name = "symbol", nullable = false, length = 5)
    private String symbol;

    @Column(name = "is_base_currency", nullable = false)
    private boolean baseCurrency;
}
