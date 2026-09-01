-- =====================================================================
-- V14__backup_restore.sql
-- Backup & Restore module.
--
-- backup_logs is an append-only operational log, not a business record --
-- mirrors audit_logs' precedent (see V1__init.sql) rather than extending
-- the standard BaseEntity shape: no is_active/deleted_at/version columns,
-- since a row is either kept as history or genuinely removed (manual
-- cleanup, or automatic retention per hfms.backup.retention-daily/
-- retention-monthly) -- never soft-deleted or edited in place.
--
-- triggered_by is NULL for an AUTOMATIC (nightly, scheduled) backup and
-- the acting username for a MANUAL ("Backup Now") one.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE backup_logs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_type       VARCHAR(20)   NOT NULL,
    file_name         VARCHAR(255)  NOT NULL,
    file_size_bytes   BIGINT        NULL,
    status            VARCHAR(20)   NOT NULL,
    error_message     VARCHAR(2000) NULL,
    started_at        DATETIME      NOT NULL,
    completed_at      DATETIME      NULL,
    triggered_by      VARCHAR(50)   NULL,

    CONSTRAINT chk_backup_logs_type CHECK (backup_type IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT chk_backup_logs_status CHECK (status IN ('SUCCESS', 'FAILED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_backup_logs_started_at ON backup_logs (started_at);
CREATE INDEX idx_backup_logs_status ON backup_logs (status);
