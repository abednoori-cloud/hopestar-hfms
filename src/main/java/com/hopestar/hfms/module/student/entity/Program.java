package com.hopestar.hfms.module.student.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Master price list entry a {@link Student} enrolls into. A student's
 * {@link StudentContract} price defaults from {@link #basePrice} but may
 * be discounted per-student, per the approved database design §2.2.
 */
@Getter
@Setter
@Entity
@Table(name = "programs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_programs_name_country", columnNames = {"name", "destination_country"})
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Program extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @NotNull
    @DecimalMin(value = "0.0", message = "Base price cannot be negative")
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "currency_code", nullable = false, length = 3)
    private SupportedCurrency currencyCode;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;
}
