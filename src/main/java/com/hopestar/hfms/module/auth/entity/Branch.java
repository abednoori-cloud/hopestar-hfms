package com.hopestar.hfms.module.auth.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A physical office/branch of the consultancy. Referenced by
 * {@code students}, {@code employees}, {@code expenses} and
 * {@code transactions} in the approved database design (§2.1) so that
 * "Multi-branch Support" (Future Module) is a data/config change rather
 * than a schema rewrite (§8 Extensibility). Exactly one row
 * (the headquarters) exists at go-live.
 */
@Getter
@Setter
@Entity
@Table(name = "branches", uniqueConstraints = {
        @UniqueConstraint(name = "uk_branches_code", columnNames = "branch_code")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Branch extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "website", length = 255)
    private String website;

    /**
     * Path to an uploaded logo image, managed by {@code FileStorageService}
     * the same way {@code StudentDocument.filePath} is. {@code null} means
     * no logo has been uploaded yet -- {@code LogoServiceImpl} falls back
     * to the bundled classpath default in that case.
     */
    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @Column(name = "is_headquarters", nullable = false)
    @Builder.Default
    private boolean headquarters = false;
}
