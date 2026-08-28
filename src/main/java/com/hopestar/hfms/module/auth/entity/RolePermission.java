package com.hopestar.hfms.module.auth.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Explicit many-to-many join entity between {@link Role} and
 * {@link Permission}, modeled as its own entity (rather than a plain
 * {@code @ManyToMany}) so it can carry its own audit trail and be queried
 * directly by the future permission-management screens, per the approved
 * database design §2.1.
 */
@Getter
@Setter
@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_permissions", columnNames = {"role_id", "permission_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"role", "permission"})
public class RolePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_role_permissions_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_role_permissions_permission"))
    private Permission permission;
}
