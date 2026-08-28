package com.hopestar.hfms.module.finance.ledger.entity;

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
 * A method by which money moved for a {@link Transaction} (Cash, Bank
 * Transfer, Credit Card, Debit Card, Mobile Wallet, Cheque, Other). Kept
 * as a database-driven lookup table rather than a hardcoded enum, per the
 * approved architecture's Suggested Improvements §1.2.3, so the owner can
 * add a new method later as a data change, not a redeploy.
 */
@Getter
@Setter
@Entity
@Table(name = "payment_methods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_methods_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_payment_methods_code", columnNames = "code")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class PaymentMethod extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Size(max = 20)
    @Column(name = "code", length = 20)
    private String code;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
}
