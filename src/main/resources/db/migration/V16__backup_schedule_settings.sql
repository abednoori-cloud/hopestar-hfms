-- =====================================================================
-- V16__backup_schedule_settings.sql
-- Backup Schedule Control.
--
-- auto_backup_enabled/auto_backup_cron move here from application.yml --
-- a static config file cannot be safely edited by a running application,
-- and Spring's @Scheduled cron-from-property-placeholder form only ever
-- resolves that placeholder once at startup, so the schedule has to be
-- real, persisted, admin-editable data for BackupScheduler to reschedule
-- live via the existing TaskScheduler bean (SchedulerConfig) without a
-- restart. (Note for future migration authors: avoid dollar-brace
-- placeholder-style text in this file's comments -- Flyway's own
-- placeholder resolver scans SQL comments too, not just executable
-- statements, and will fail the migration on an unrecognized one.)
--
-- Single-row table (mirrors branches' single-HQ-row pattern) rather than
-- a generic key/value settings table -- this codebase has deliberately
-- avoided a catch-all settings table so far (see BackupProperties'
-- Javadoc on the removed retention config). Extends the standard
-- BaseEntity shape (unlike backup_logs' append-only AuditLog-style
-- shape): this is a genuinely mutable, admin-edited record, not a log.
--
-- mysqldump-path/mysql-restore-path deliberately stay in application.yml
-- -- machine/environment-specific install paths, not a business setting
-- an office admin should edit through a web form.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE backup_schedule_settings (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    auto_backup_enabled    BOOLEAN        NOT NULL,
    auto_backup_cron       VARCHAR(100)   NOT NULL,

    is_active              BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at             DATETIME       NULL,
    created_at             DATETIME       NOT NULL,
    updated_at             DATETIME       NULL,
    created_by             VARCHAR(50)    NULL,
    updated_by             VARCHAR(50)    NULL,
    version                BIGINT         NOT NULL DEFAULT 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Matches the current application.yml/application-dev.yml effective
-- defaults exactly (disabled, 02:00 daily) so behavior is unchanged until
-- an admin explicitly opts in via the new Settings UI.
INSERT INTO backup_schedule_settings
    (auto_backup_enabled, auto_backup_cron, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    (FALSE, '0 0 2 * * *', TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');
