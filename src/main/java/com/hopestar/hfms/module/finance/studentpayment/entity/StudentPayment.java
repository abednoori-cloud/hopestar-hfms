package com.hopestar.hfms.module.finance.studentpayment.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.student.entity.Student;
import com.hopestar.hfms.module.student.entity.StudentContract;
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
 * A payment a student makes against one of their {@link StudentContract}s.
 * Per the approved Finance module architecture, this entity never posts
 * to the ledger itself -- a {@code POSTED} payment always has a
 * corresponding {@link Transaction} row, created exclusively by {@link
 * com.hopestar.hfms.module.finance.ledger.service.LedgerService#postIncome}.
 * A {@code DRAFT} payment has {@link #transaction} {@code null} until it
 * is posted.
 * <p>
 * <b>Currency handling</b> follows the same pattern established by {@link
 * StudentContract} and the ledger: {@link #originalAmount}/{@link
 * #currency} are exactly as entered; {@link #exchangeRateToUsd} is
 * manually entered (always {@code 1.000000} for USD) and never rewritten
 * after posting; {@link #usdEquivalentAmount} is computed once by {@code
 * StudentPaymentServiceImpl} and is what contract-balance and reporting
 * calculations use.
 */
@Getter
@Setter
@Entity
@Table(name = "student_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_payments_payment_number", columnNames = "payment_number"),
        @UniqueConstraint(name = "uk_student_payments_receipt_number", columnNames = "receipt_number")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"student", "contract", "transaction", "currency", "paymentMethod"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class StudentPayment extends BaseEntity {

    /** System-generated, e.g. {@code PAY-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "payment_number", nullable = false, length = 30, updatable = false)
    private String paymentNumber;

    /** System-generated, e.g. {@code RCPT-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "receipt_number", nullable = false, length = 30, updatable = false)
    private String receiptNumber;

    /**
     * Plain nullable column, not a foreign key: the Invoice module does
     * not exist yet (a later phase). See this entity's companion
     * migration, {@code V6__student_payments.sql}.
     */
    @Column(name = "invoice_id")
    private Long invoiceId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_payments_student"))
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_payments_contract"))
    private StudentContract contract;

    /** Null while {@link #status} is {@code DRAFT}; set once the payment is posted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", foreignKey = @ForeignKey(name = "fk_student_payments_transaction"))
    private Transaction transaction;

    @NotNull
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @NotNull
    @DecimalMin(value = "0.0", message = "Original amount cannot be negative")
    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_payments_currency"))
    private Currency currency;

    @NotNull
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    @Builder.Default
    @Column(name = "exchange_rate_to_usd", nullable = false, precision = 14, scale = 6)
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    /** {@code originalAmount * exchangeRateToUsd}, computed by {@code StudentPaymentServiceImpl}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent amount cannot be negative")
    @Column(name = "usd_equivalent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentAmount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_payments_payment_method"))
    private PaymentMethod paymentMethod;

    @Size(max = 100)
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.POSTED;
}
