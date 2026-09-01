package com.hopestar.hfms.common.enums;

/**
 * Outcome of a single {@code backup_logs} row, recorded by {@code
 * BackupServiceImpl} once the {@code mysqldump} process exits. Kept
 * alongside {@link BackupType} in {@code common/enums} rather than under
 * the backup module's own entity package, matching where Phase 1 already
 * placed the sibling enum.
 */
public enum BackupStatus {
    SUCCESS,
    FAILED
}
