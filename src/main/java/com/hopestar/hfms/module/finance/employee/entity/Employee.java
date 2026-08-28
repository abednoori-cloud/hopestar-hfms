package com.hopestar.hfms.module.finance.employee.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.SupportedCurrency;
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
import jakarta.validation.constraints.Email;
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
 * A member of staff, per SRS Module 5 (Employee Management). Never
 * hard-deleted -- only deactivated via {@link BaseEntity#softDelete()} --
 * per the approved business rules.
 * <p>
 * <b>Currency handling</b> follows the exact pattern established by
 * {@code StudentContract} and {@code StudentPayment}: {@link
 * #baseSalary}/{@link #salaryCurrency} are exactly as entered; {@link
 * #exchangeRateToUsd} is manually entered (always {@code 1.000000} for
 * USD, enforced by {@code MoneyUtil.resolveExchangeRateToUsd} -- the one
 * shared implementation of this rule, never duplicated here); {@link
 * #usdEquivalentSalary} is computed once by {@code EmployeeServiceImpl}
 * and is what Reports/Dashboard (later phases) will read from.
 */
@Getter
@Setter
@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employees_employee_code", columnNames = "employee_code"),
        @UniqueConstraint(name = "uk_employees_phone", columnNames = "phone"),
        @UniqueConstraint(name = "uk_employees_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_employees_national_id", columnNames = "national_id")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"employmentStatus", "branch"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Employee extends BaseEntity {

    /** System-generated, e.g. {@code EMP-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 20)
    @Column(name = "employee_code", nullable = false, length = 20, updatable = false)
    private String employeeCode;

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Size(max = 150)
    @Column(name = "father_name", length = 150)
    private String fatherName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Email
    @Size(max = 100)
    @Column(name = "email", length = 100)
    private String email;

    @Size(max = 50)
    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Size(max = 255)
    @Column(name = "address", length = 255)
    private String address;

    @NotBlank
    @Size(max = 100)
    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @NotBlank
    @Size(max = 100)
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @NotNull
    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employees_status"))
    private EmployeeStatus employmentStatus;

    @NotNull
    @DecimalMin(value = "0.0", message = "Base salary cannot be negative")
    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "salary_currency", nullable = false, length = 3)
    private SupportedCurrency salaryCurrency = SupportedCurrency.USD;

    @NotNull
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    @Builder.Default
    @Column(name = "exchange_rate_to_usd", nullable = false, precision = 14, scale = 6)
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    /** {@code baseSalary * exchangeRateToUsd}, computed by {@code EmployeeServiceImpl}. */
    @NotNull
    @DecimalMin(value = "0.0", message = "USD equivalent salary cannot be negative")
    @Column(name = "usd_equivalent_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal usdEquivalentSalary;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "fk_employees_branch"))
    private Branch branch;
}
