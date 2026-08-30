package com.hopestar.hfms.module.finance.expense.entity;

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
 * A classification for an {@link Expense} (Rent, Electricity, Internet,
 * Office Supplies, Transportation, Other). Kept as a database-driven
 * lookup table rather than a hardcoded enum, mirroring {@code
 * PaymentMethod}'s precedent, so the owner can add a category later as a
 * data change, not a redeploy.
 */
@Getter
@Setter
@Entity
@Table(name = "expense_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_expense_categories_name", columnNames = "name")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class ExpenseCategory extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
