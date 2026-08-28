package com.hopestar.hfms.module.student.entity;

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
 * Lookup table for a student's current lifecycle status (Active, On Hold,
 * Completed, Cancelled, Deferred). Kept as a database-driven table rather
 * than a hardcoded enum, per the approved architecture's Suggested
 * Improvements §1.2.3 — the owner can introduce a new status (e.g. "Visa
 * Approved") as a data change, without a redeploy.
 */
@Getter
@Setter
@Entity
@Table(name = "student_statuses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_statuses_name", columnNames = "name")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class StudentStatus extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
}
