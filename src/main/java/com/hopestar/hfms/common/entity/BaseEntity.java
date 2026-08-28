package com.hopestar.hfms.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class for every JPA entity in the system.
 * <p>
 * Provides the surrogate primary key, the standard audit columns
 * (created_at/updated_at/created_by/updated_by), the soft-delete flag
 * (is_active / deleted_at) and optimistic-locking version column that the
 * approved database design (SRS Architecture §2) requires on every table.
 * <p>
 * Financial/master entities extend this class rather than declaring these
 * columns individually, keeping the soft-delete and audit behaviour
 * uniform across all modules as mandated by the architecture's
 * "Suggested Improvements" §1.2.1.
 * <p>
 * Annotated with Lombok's {@code @SuperBuilder} (rather than plain
 * {@code @Builder}) specifically so every subclass across every module can
 * itself use {@code @SuperBuilder} and inherit these fields into its own
 * builder — {@code @SuperBuilder} requires every class in the hierarchy,
 * including this one, to carry the annotation.
 */
@Getter
@Setter
@MappedSuperclass
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /**
     * Optimistic locking column. Prevents two concurrent edits (e.g. two
     * browser tabs on the single-admin desktop today, or two users once
     * multi-user access lands per the Future Modules roadmap) from
     * silently overwriting each other's changes.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /**
     * Soft-deletes this record. Financial and master-data rows are never
     * hard-deleted per the approved business rules; the record is flagged
     * inactive and timestamped instead, and remains fully queryable for
     * audit and history purposes.
     */
    public void softDelete() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.active = true;
        this.deletedAt = null;
    }
}
