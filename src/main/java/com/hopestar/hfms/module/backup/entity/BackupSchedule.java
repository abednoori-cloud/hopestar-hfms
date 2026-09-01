package com.hopestar.hfms.module.backup.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The single (id fixed at 1, seeded by {@code V16__backup_schedule_settings.sql})
 * row controlling the nightly automatic backup job. Unlike {@link
 * com.hopestar.hfms.module.backup.entity.BackupLog} (an append-only log),
 * this is a genuinely mutable, admin-edited setting -- extends {@link
 * BaseEntity} the same way {@code Branch} does for its single HQ row,
 * rather than the AuditLog-style shape.
 * <p>
 * Persisted here (not {@code application.yml}) specifically so {@code
 * BackupScheduler} can reschedule the job live via the existing {@code
 * TaskScheduler} bean when an admin changes it, without an application
 * restart -- see {@code BackupScheduler}'s Javadoc.
 */
@Getter
@Setter
@Entity
@Table(name = "backup_schedule_settings")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class BackupSchedule extends BaseEntity {

    @Column(name = "auto_backup_enabled", nullable = false)
    private boolean autoBackupEnabled;

    /** Spring's 6-field cron form, e.g. {@code "0 0 2 * * *"} for 02:00 daily. */
    @Column(name = "auto_backup_cron", nullable = false, length = 100)
    private String autoBackupCron;
}
