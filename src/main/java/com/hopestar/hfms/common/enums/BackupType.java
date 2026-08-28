package com.hopestar.hfms.common.enums;

/**
 * Distinguishes a scheduled automatic backup from an owner-triggered
 * manual backup in {@code backup_logs}. Both paths share the same
 * underlying BackupService implementation (see Module 8 in the approved
 * architecture) so this flag is for reporting/audit purposes only.
 */
public enum BackupType {
    MANUAL,
    AUTOMATIC
}
