package com.hopestar.hfms.module.finance.advance.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.employee.entity.Employee;
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
 * A cash advance given to an employee. Lifecycle: {@code DRAFT} (not yet
 * disbursed, freely editable) -&gt; {@code ACTIVE} (disbursed -- {@link
 * #ledgerTransaction} set exclusively by {@code LedgerService.postExpense})
 * -&gt; {@code CLOSED} (repaid in full, {@link #remainingBalance} reached
 * zero) or {@code VOID} (cancelled while {@code DRAFT}, or disbursed then
 * voided via {@code LedgerService.voidTransaction}).
 * <p>
 * Mirrors {@code EmployeeLoan} exactly, minus the fixed monthly-deduction
 * repayment schedule (start/end month/year) a formal loan has -- an
 * advance is a smaller, shorter-term amount repaid ad hoc rather than on
 * a fixed installment plan.
 * <p>
 * <b>Currency handling</b> follows the exact pattern established by
 * {@code EmployeeLoan}/{@code Employee}: {@link #advanceAmount}/{@link
 * #currency} are exactly as entered; {@link #exchangeRateToUsd} is
 * manually entered (always {@code 1.000000} for USD, enforced by the
 * single shared {@code MoneyUtil.resolveExchangeRateToUsd}); {@link
 * #usdEquivalentAmount} is computed once at create/update time.
 * <p>
 * Per the approved business rules, this entity never posts to the ledger
 * itself -- an {@code ACTIVE} advance always has a corresponding {@link
 * Transaction}, created exclusively by {@code LedgerService.postExpense}.
 */
@Getter
@Setter
@Entity
@Table(name = "employee_advances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_advances_advance_number", columnNames = "advance_number")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"employee", "ledgerTransaction"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class EmployeeAdvance extends BaseEntity {

    /** System-generated, e.g. {@code ADV-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "advance_number", nullable = false, length = 30, updatable = false)
    private String advanceNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_advances_employee"))
    private Employee employee;

    @NotNull
    @Positive(message = "Advance amount must be greater than zero")
    @Column(name = "advance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceAmount;

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

    /** {@code advanceAmount * exchangeRateToUsd}, computed by {@code EmployeeAdvanceServiceImpl}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent amount cannot be negative")
    @Column(name = "usd_equivalent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentAmount;

    /** Starts equal to {@link #advanceAmount} at creation, decremented by each posted repayment. */
    @NotNull
    @DecimalMin(value = "0.0", message = "Remaining balance cannot be negative")
    @Column(name = "remaining_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingBalance;

    @NotNull
    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private AdvanceStatus status = AdvanceStatus.DRAFT;

    @Size(max = 2000)
    @Column(name = "remarks", length = 2000)
    private String remarks;

    /** Null while {@link #status} is {@code DRAFT}; set once the advance is disbursed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_transaction_id", foreignKey = @ForeignKey(name = "fk_employee_advances_ledger_transaction"))
    private Transaction ledgerTransaction;
}
