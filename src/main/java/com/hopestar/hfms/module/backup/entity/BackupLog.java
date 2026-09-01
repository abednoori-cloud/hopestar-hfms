package com.hopestar.hfms.module.backup.entity;

import com.hopestar.hfms.common.enums.BackupStatus;
import com.hopestar.hfms.common.enums.BackupType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only record of a single backup attempt (manual or automatic).
 * Deliberately does <b>not</b> extend {@code BaseEntity} -- mirrors {@code
 * com.hopestar.hfms.audit.entity.AuditLog}'s precedent exactly: a row is
 * either kept as history or genuinely removed (manual delete via {@code
 * BackupService.deleteBackup}, or automatic retention cleanup), never
 * soft-deleted or edited in place.
 */
@Getter
@Setter
@Entity
@Table(name = "backup_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", nullable = false, length = 20)
    private BackupType backupType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BackupStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Null for an {@code AUTOMATIC} backup; the acting username for a {@code MANUAL} one. */
    @Column(name = "triggered_by", length = 50)
    private String triggeredBy;
}
