package com.hopestar.hfms.module.finance.salary.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.employee.entity.Employee;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * One employee's salary record for one calendar month, per SRS Module 5
 * (Salary). Exactly one record may exist per {@code (employee, month,
 * year)} triple, enforced both by {@code uk_salaries_employee_month_year}
 * and by {@code SalaryServiceImpl}'s duplicate check.
 * <p>
 * {@link #netSalary} is always computed by {@code SalaryServiceImpl} as
 * {@code basicSalary + bonus + overtimeAmount + allowance - penalty -
 * loanDeduction - advanceDeduction - otherDeduction}; never entered
 * manually.
 * <p>
 * <b>Currency handling</b> follows the exact pattern established by
 * {@code Employee}/{@code StudentContract}/{@code StudentPayment}: {@link
 * #basicSalary}/{@link #currency} are exactly as entered; {@link
 * #exchangeRateToUsd} is manually entered (always {@code 1.000000} for
 * USD, enforced by the single shared {@code MoneyUtil.resolveExchangeRateToUsd});
 * {@link #usdEquivalentSalary} is the USD equivalent of {@link
 * #basicSalary} specifically (mirroring {@code Employee.usdEquivalentSalary}),
 * computed once at create/update time. The USD equivalent of {@link
 * #netSalary} is not a separate stored field -- once posted, it is
 * available from {@link #ledgerTransaction}'s own
 * {@code usdEquivalentAmount}, computed independently by {@code
 * LedgerService} using this same {@link #exchangeRateToUsd}, so the
 * figure is never duplicated ahead of time.
 * <p>
 * Per the approved business rules, this entity never posts to the ledger
 * itself -- a {@code POSTED} salary always has a corresponding {@link
 * Transaction}, created exclusively by {@code LedgerService.postExpense}.
 */
@Getter
@Setter
@Entity
@Table(name = "salaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_salaries_salary_number", columnNames = "salary_number"),
        @UniqueConstraint(name = "uk_salaries_employee_month_year", columnNames = {"employee_id", "salary_month", "salary_year"})
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"employee", "paymentMethod", "ledgerTransaction"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Salary extends BaseEntity {

    /** System-generated, e.g. {@code SAL-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "salary_number", nullable = false, length = 30, updatable = false)
    private String salaryNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_salaries_employee"))
    private Employee employee;

    @NotNull
    @Min(1)
    @Max(12)
    @Column(name = "salary_month", nullable = false)
    private Integer month;

    @NotNull
    @Min(2000)
    @Max(2100)
    @Column(name = "salary_year", nullable = false)
    private Integer year;

    @NotNull
    @DecimalMin(value = "0.0", message = "Basic salary cannot be negative")
    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

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

    /** {@code basicSalary * exchangeRateToUsd}, computed by {@code SalaryServiceImpl}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent salary cannot be negative")
    @Column(name = "usd_equivalent_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentSalary;

    @NotNull
    @DecimalMin(value = "0.0", message = "Bonus cannot be negative")
    @Builder.Default
    @Column(name = "bonus", nullable = false, precision = 12, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Overtime amount cannot be negative")
    @Builder.Default
    @Column(name = "overtime_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal overtimeAmount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Allowance cannot be negative")
    @Builder.Default
    @Column(name = "allowance", nullable = false, precision = 12, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Penalty cannot be negative")
    @Builder.Default
    @Column(name = "penalty", nullable = false, precision = 12, scale = 2)
    private BigDecimal penalty = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Loan deduction cannot be negative")
    @Builder.Default
    @Column(name = "loan_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal loanDeduction = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Advance deduction cannot be negative")
    @Builder.Default
    @Column(name = "advance_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Other deduction cannot be negative")
    @Builder.Default
    @Column(name = "other_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    /**
     * {@code basicSalary + bonus + overtimeAmount + allowance - penalty -
     * loanDeduction - advanceDeduction - otherDeduction}, computed by
     * {@code SalaryServiceImpl}. Never entered manually.
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "Net salary cannot be negative")
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 20)
    private SalaryPaymentStatus paymentStatus = SalaryPaymentStatus.DRAFT;

    /** Null until posted; set to the posting date when {@link #paymentStatus} becomes {@code POSTED}. */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false, foreignKey = @ForeignKey(name = "fk_salaries_payment_method"))
    private PaymentMethod paymentMethod;

    /** Null while {@link #paymentStatus} is {@code DRAFT}; set once the salary is posted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_transaction_id", foreignKey = @ForeignKey(name = "fk_salaries_ledger_transaction"))
    private Transaction ledgerTransaction;

    @Size(max = 2000)
    @Column(name = "remarks", length = 2000)
    private String remarks;
}
