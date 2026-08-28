package com.hopestar.hfms.module.finance.ledger.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.module.auth.entity.Branch;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
 * The central ledger row. Every financial event in the system — student
 * payments, English test payments, salaries, expenses, loans, advances,
 * refunds, invoices — is represented here, and here only. Per the
 * approved architecture's Finance module design, no submodule ever
 * inserts a {@code Transaction} directly; every one of them is created
 * by {@link com.hopestar.hfms.module.finance.ledger.service.LedgerService},
 * which is the single point that computes {@link #usdEquivalentAmount}
 * and enforces the exchange-rate business rules. This is what keeps the
 * Dashboard and Reports (later phases) internally consistent: they read
 * from this one table instead of aggregating five independent module
 * tables that could drift out of sync.
 * <p>
 * {@link #referenceTable}/{@link #referenceId} form a polymorphic,
 * loosely-coupled link back to the originating record (e.g.
 * {@code student_contracts}) without this entity taking a hard JPA
 * relationship to every submodule's entity — the Finance module must not
 * depend on modules built after it.
 * <p>
 * <b>Currency handling:</b> {@link #originalAmount}/{@link #currency} are
 * the amount and currency exactly as entered; {@link #exchangeRateToUsd}
 * is entered manually by the user (always {@code 1.000000} for USD) and
 * is never rewritten after the fact — each transaction is its own
 * historical snapshot, even if a later transaction uses a different rate
 * for the same currency; {@link #usdEquivalentAmount} is computed once at
 * creation time and is what Reports/Dashboard sum.
 */
@Getter
@Setter
@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_code", columnNames = "transaction_code")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"currency", "paymentMethod", "branch"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Transaction extends BaseEntity {

    /** System-generated, e.g. {@code TXN-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "transaction_code", nullable = false, length = 30, updatable = false)
    private String transactionCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private Direction direction;

    @NotNull
    @DecimalMin(value = "0.0", message = "Original amount cannot be negative")
    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_currency"))
    private Currency currency;

    /**
     * Entered manually by the user (no live FX feed). Always
     * {@code 1.000000} when {@link #currency} is USD. Immutable in
     * practice once posted — see this class's Javadoc.
     */
    @NotNull
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    @Builder.Default
    @Column(name = "exchange_rate_to_usd", nullable = false, precision = 14, scale = 6)
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    /** {@code originalAmount * exchangeRateToUsd}, computed by {@code LedgerService}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent amount cannot be negative")
    @Column(name = "usd_equivalent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentAmount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_payment_method"))
    private PaymentMethod paymentMethod;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /**
     * Polymorphic link back to the originating record's table (e.g.
     * {@code student_contracts}), so the Finance module never takes a
     * hard entity dependency on the modules that call it.
     */
    @Size(max = 100)
    @Column(name = "reference_table", length = 100)
    private String referenceTable;

    @Column(name = "reference_id")
    private Long referenceId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.POSTED;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "fk_transactions_branch"))
    private Branch branch;
}
