package com.hopestar.hfms.module.auth.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A granular permission code (e.g. {@code STUDENT_VIEW}, {@code SALARY_APPROVE},
 * {@code BACKUP_RESTORE}) that can be attached to one or more roles via
 * {@link RolePermission}. This exists from Phase 1 so that granular,
 * privileged-operation gating (approved architecture §5.2 — voiding a
 * transaction, restoring a backup, etc.) is available the moment
 * multi-role access is introduced, without a schema change.
 */
@Getter
@Setter
@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_code", columnNames = "code")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "module", length = 50)
    private String module;
}
