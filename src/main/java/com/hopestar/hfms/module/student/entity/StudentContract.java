package com.hopestar.hfms.module.student.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A pricing agreement for one {@link Student} against one {@link Program}.
 * A student may have multiple contracts over time (re-enrollment, program
 * change), per the approved business rules.
 * <p>
 * {@link #finalAmount} is computed once at create/update time as
 * {@code totalContractAmount - discountAmount} — a static contract term,
 * not something derived from payment history, so storing it (rather than
 * always recomputing) is safe and matches the approved database design
 * §2.2. {@code remainingBalance} is deliberately <b>not</b> a column on
 * this entity: per the approved architecture's ledger design (§2.4/§2.5),
 * remaining balance is computed as {@code finalAmount} minus the sum of
 * posted {@code student_payments}. The Finance module (a later phase)
 * has not been implemented yet, so today's remaining balance is simply
 * the full {@code finalAmount}; {@link com.hopestar.hfms.module.student.service.StudentContractService}
 * documents this and is the single place that calculation will change
 * once student_payments exists.
 * <p>
 * <b>Currency handling:</b> the system's base currency is USD. A contract
 * may be priced in any of {@link SupportedCurrency} (USD, EUR, AFN);
 * {@link #exchangeRateToUsd} is entered manually by the user at the time
 * the contract is created (this system does not integrate a live FX feed)
 * and is stored permanently alongside the original amount/currency so the
 * conversion used at the time of the transaction is never lost, even if
 * the operator enters a different rate on a later contract. {@link
 * #usdEquivalentAmount} ({@code finalAmount * exchangeRateToUsd}) is what
 * Reports and the Dashboard read from in later phases; the original
 * amount and currency remain available on this same row for display.
 */
@Getter
@Setter
@Entity
@Table(name = "student_contracts")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"student", "program"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class StudentContract extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_contracts_student"))
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_contracts_program"))
    private Program program;

    @NotNull
    @DecimalMin(value = "0.0", message = "Total contract amount cannot be negative")
    @Column(name = "total_contract_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalContractAmount;

    @NotNull
    @DecimalMin(value = "0.0", message = "Registration fee cannot be negative")
    @Builder.Default
    @Column(name = "registration_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal registrationFee = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Discount amount cannot be negative")
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 255)
    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    /** {@code totalContractAmount - discountAmount}, computed by the service layer. */
    @NotNull
    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    /** Original currency the contract was priced in. Base currency is USD. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 3)
    private SupportedCurrency currencyCode = SupportedCurrency.USD;

    /**
     * Manually entered by the user at contract-creation time (this system
     * does not integrate a live FX feed). Always {@code 1.000000} when
     * {@link #currencyCode} is {@code USD}.
     */
    @NotNull
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    @Builder.Default
    @Column(name = "exchange_rate_to_usd", nullable = false, precision = 14, scale = 6)
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    /**
     * {@code finalAmount * exchangeRateToUsd}, computed by the service
     * layer at create/update time and stored permanently — this is what
     * Reports and the Dashboard (later phases) read from, per the
     * project's currency-handling requirement, while {@link #finalAmount}
     * and {@link #currencyCode} remain available for original-currency
     * display.
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent amount cannot be negative")
    @Column(name = "usd_equivalent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentAmount;

    @NotNull
    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;
}
