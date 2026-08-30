package com.hopestar.hfms.module.finance.advance.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
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
import jakarta.validation.constraints.Positive;
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
 * One repayment posted against an {@link EmployeeAdvance} -- the auditable
 * history behind {@link EmployeeAdvance#getRemainingBalance()}. Each row
 * has its own {@link #currency}/{@link #exchangeRateToUsd}/{@link
 * #usdEquivalentAmount} independent of the parent advance's, exactly like
 * {@code LoanRepaymentSchedule} versus its {@code EmployeeLoan}.
 * <p>
 * Per the approved business rules, this entity never posts to the ledger
 * itself -- every row always has a corresponding {@link Transaction},
 * created exclusively by {@code LedgerService.postIncome}.
 */
@Getter
@Setter
@Entity
@Table(name = "advance_repayments")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"advance", "paymentMethod", "ledgerTransaction"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class AdvanceRepaymentSchedule extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_advance_repayments_advance"))
    private EmployeeAdvance advance;

    @NotNull
    @Column(name = "repayment_date", nullable = false)
    private LocalDate repaymentDate;

    @NotNull
    @Positive(message = "Repayment amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 3)
    private SupportedCurrency currency = SupportedCurrency.USD;

    @NotNull
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    @Builder.Default
    @Column(name = "exchange_rate_to_usd", nullable = false, precision = 14, scale = 6)
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    /** {@code amount * exchangeRateToUsd}, computed by {@code EmployeeAdvanceServiceImpl}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent amount cannot be negative")
    @Column(name = "usd_equivalent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentAmount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false, foreignKey = @ForeignKey(name = "fk_advance_repayments_payment_method"))
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_transaction_id", foreignKey = @ForeignKey(name = "fk_advance_repayments_ledger_transaction"))
    private Transaction ledgerTransaction;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;
}
