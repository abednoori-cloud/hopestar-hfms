package com.hopestar.hfms.module.backup.service;

import com.hopestar.hfms.common.enums.BackupType;
import com.hopestar.hfms.module.backup.dto.BackupLogResponseDTO;

import java.util.List;

/**
 * Database backup and restore, by shelling out to the MySQL-bundled
 * {@code mysqldump}/{@code mysql} command-line tools (see {@code
 * BackupServiceImpl}'s Javadoc for why). Backs up the application
 * database only -- uploaded student documents under {@code
 * hfms.file-storage.documents-path} are out of scope, per the approved
 * requirements.
 */
public interface BackupService {

    /**
     * Runs {@code mysqldump} against the configured database and writes a
     * timestamped {@code .sql} file to {@code hfms.file-storage.backup-path}.
     * Always records a {@code backup_logs} row, SUCCESS or FAILED, before
     * returning or throwing.
     *
     * @param triggeredBy the acting username for a {@link BackupType#MANUAL}
     *                     backup; {@code null} for {@link BackupType#AUTOMATIC}
     * @throws com.hopestar.hfms.common.exception.BusinessValidationException
     *         if the dump process fails or times out (the failure is still
     *         logged to {@code backup_logs} before this is thrown)
     */
    BackupLogResponseDTO createBackup(BackupType type, String triggeredBy);

    /**
     * Restores the database from a previously successful backup by piping
     * its dump file into {@code mysql}. Genuinely destructive -- overwrites
     * the live database -- so callers (the controller) are expected to have
     * obtained an explicit, deliberate confirmation before invoking this.
     *
     * @throws com.hopestar.hfms.common.exception.ResourceNotFoundException
     *         if no backup log with this id exists
     * @throws com.hopestar.hfms.common.exception.BusinessValidationException
     *         if the referenced backup did not succeed, its file is
     *         missing, or the restore process fails or times out
     */
    void restoreBackup(Long backupLogId, String restoredBy);

    /** Most recent first. */
    List<BackupLogResponseDTO> listBackups();

    /**
     * Deletes a backup's dump file (if still present) and its {@code
     * backup_logs} row -- a real row deletion, not a soft-delete; see
     * {@link com.hopestar.hfms.module.backup.entity.BackupLog}'s Javadoc.
     */
    void deleteBackup(Long backupLogId);
}
