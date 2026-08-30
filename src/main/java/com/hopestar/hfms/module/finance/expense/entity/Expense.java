package com.hopestar.hfms.module.finance.expense.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
 * A simple office expense (rent, electricity, supplies, ...) -- not a
 * person-to-person transaction like {@code EmployeeLoan}/{@code
 * EmployeeAdvance}, so there is no repayment/schedule concept at all: an
 * expense is posted once and that's it. Lifecycle: {@code DRAFT} (freely
 * editable) -&gt; {@code POSTED} (posted to the ledger -- {@link
 * #ledgerTransaction} set exclusively by {@code LedgerService.postExpense}
 * -- and done, since there is no ongoing balance to track) or {@code
 * VOID} (cancelled while {@code DRAFT}, or posted then voided via {@code
 * LedgerService.voidTransaction}).
 * <p>
 * <b>Currency handling</b> follows the exact pattern established by
 * {@code EmployeeAdvance}/{@code EmployeeLoan}: {@link #amount}/{@link
 * #currency} are exactly as entered; {@link #exchangeRateToUsd} is
 * manually entered (always {@code 1.000000} for USD, enforced by the
 * single shared {@code MoneyUtil.resolveExchangeRateToUsd}); {@link
 * #usdEquivalentAmount} is computed once at create/update time.
 * <p>
 * Per the approved business rules, this entity never posts to the ledger
 * itself -- a {@code POSTED} expense always has a corresponding {@link
 * Transaction}, created exclusively by {@code LedgerService.postExpense}.
 */
@Getter
@Setter
@Entity
@Table(name = "expenses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_expenses_expense_number", columnNames = "expense_number")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"category", "ledgerTransaction"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Expense extends BaseEntity {

    /** System-generated, e.g. {@code EXP-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "expense_number", nullable = false, length = 30, updatable = false)
    private String expenseNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_expenses_category"))
    private ExpenseCategory category;

    @NotBlank
    @Size(max = 500)
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @NotNull
    @Positive(message = "Amount must be greater than zero")
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

    /** {@code amount * exchangeRateToUsd}, computed by {@code ExpenseServiceImpl}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent amount cannot be negative")
    @Column(name = "usd_equivalent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentAmount;

    @NotNull
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Size(max = 2000)
    @Column(name = "remarks", length = 2000)
    private String remarks;

    /** Null while {@link #status} is {@code DRAFT}; set once the expense is posted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_transaction_id", foreignKey = @ForeignKey(name = "fk_expenses_ledger_transaction"))
    private Transaction ledgerTransaction;
}
