package com.hopestar.hfms.module.backup.service;

import com.hopestar.hfms.common.enums.BackupStatus;
import com.hopestar.hfms.common.enums.BackupType;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.config.BackupProperties;
import com.hopestar.hfms.config.FileStorageProperties;
import com.hopestar.hfms.module.backup.dto.BackupLogResponseDTO;
import com.hopestar.hfms.module.backup.entity.BackupLog;
import com.hopestar.hfms.module.backup.repository.BackupLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Implements {@link BackupService} by shelling out to the MySQL-bundled
 * {@code mysqldump}/{@code mysql} command-line tools via {@link
 * ProcessBuilder} -- the standard approach for MySQL backup/restore,
 * rather than reimplementing dump/restore logic in Java: {@code
 * mysqldump} already handles consistent snapshots ({@code
 * --single-transaction}), routines/triggers, and produces a
 * restore-with-plain-{@code mysql} file, all version-matched to the
 * installed server (see the Phase 2 inspection notes).
 * <p>
 * Credentials are never passed as a plain {@code -p} command-line
 * argument (visible to any other process on the machine via a process
 * listing) -- a temporary {@code --defaults-extra-file} is written before
 * each operation and deleted in a {@code finally} block immediately
 * after, even on failure.
 * <p>
 * Connection details (host/port/database/credentials) are read from the
 * same {@link DataSourceProperties} Spring Boot already binds from {@code
 * spring.datasource.*}, rather than duplicating them under {@code
 * hfms.backup.*} -- one source of truth for how to reach the database.
 * <p>
 * Deliberately <b>not</b> class- or method-level {@code @Transactional} on
 * {@link #createBackup}/{@link #restoreBackup}: each does at most one
 * repository call, which is already independently transactional via
 * {@code SimpleJpaRepository}'s own proxy -- wrapping the whole method
 * would instead check out a JDBC connection from the pool and hold it
 * open, idle, for however long the external {@code mysqldump}/{@code
 * mysql} process takes (observed firsthand during Phase 2 verification: a
 * long-blocked restore killed its own held connection after several
 * minutes idle, "Communications link failure"). {@link #deleteBackup} has
 * no external process in its critical section, so it keeps the
 * conventional {@code @Transactional}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final long PROCESS_TIMEOUT_MINUTES = 15;

    private final BackupLogRepository backupLogRepository;
    private final BackupProperties backupProperties;
    private final FileStorageProperties fileStorageProperties;
    private final DataSourceProperties dataSourceProperties;

    @Override
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public BackupLogResponseDTO createBackup(BackupType type, String triggeredBy) {
        LocalDateTime startedAt = LocalDateTime.now();
        String fileName = "hfms_backup_" + startedAt.format(FILENAME_TIMESTAMP) + ".sql";
        Path targetFile = Path.of(fileStorageProperties.getBackupPath())
                .resolve(fileName)
                .toAbsolutePath()
                .normalize();

        DbConnectionInfo connectionInfo = resolveConnectionInfo();
        Path defaultsFile = null;
        String errorMessage = null;
        long fileSizeBytes = 0;

        try {
            defaultsFile = writeDefaultsExtraFile(connectionInfo.username(), connectionInfo.password());

            List<String> command = List.of(
                    backupProperties.getMysqldumpPath(),
                    "--defaults-extra-file=" + defaultsFile.toAbsolutePath(),
                    "--host=" + connectionInfo.host(),
                    "--port=" + connectionInfo.port(),
                    "--single-transaction",
                    // Harmless with --single-transaction (InnoDB doesn't
                    // need table locks for a consistent snapshot) -- but
                    // does NOT remove mysqldump's need for the RELOAD/
                    // FLUSH_TABLES privilege: this MySQL version issues an
                    // internal FLUSH TABLES unconditionally at dump start,
                    // which is a global-only grant the DB user needs
                    // regardless of dump flags (see application-dev.yml's
                    // comment / the deployment notes on the required
                    // hfms_user grant).
                    "--skip-lock-tables",
                    // Without this, mysqldump wraps the dump in SET
                    // @@SESSION.SQL_LOG_BIN=0 / GTID_PURGED statements for
                    // replication safety -- irrelevant for this single-
                    // server local backup tool, and restoring them needs
                    // SUPER/SYSTEM_VARIABLES_ADMIN, another global-only
                    // privilege the app's DB user should not need.
                    "--set-gtid-purged=OFF",
                    "--routines",
                    "--triggers",
                    connectionInfo.database());

            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .redirectOutput(targetFile.toFile());
            runProcess(processBuilder, "mysqldump");

            fileSizeBytes = Files.size(targetFile);
        } catch (Exception ex) {
            log.error("Backup failed", ex);
            errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            deleteQuietly(targetFile);
        } finally {
            deleteQuietly(defaultsFile);
        }

        BackupLog backupLog = BackupLog.builder()
                .backupType(type)
                .fileName(fileName)
                .fileSizeBytes(errorMessage == null ? fileSizeBytes : null)
                .status(errorMessage == null ? BackupStatus.SUCCESS : BackupStatus.FAILED)
                .errorMessage(errorMessage)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
        BackupLog saved = backupLogRepository.save(backupLog);

        if (errorMessage != null) {
            throw new BusinessValidationException("Backup failed: " + errorMessage);
        }

        if (type == BackupType.AUTOMATIC) {
            applyRetention();
        }

        return toResponseDTO(saved);
    }

    @Override
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public void restoreBackup(Long backupLogId, String restoredBy) {
        BackupLog backupLog = backupLogRepository.findById(backupLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", backupLogId));

        if (backupLog.getStatus() != BackupStatus.SUCCESS) {
            throw new BusinessValidationException(
                    "Cannot restore backup '" + backupLog.getFileName() + "' because it did not complete successfully.");
        }

        Path dumpFile = Path.of(fileStorageProperties.getBackupPath())
                .resolve(backupLog.getFileName())
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(dumpFile)) {
            throw new BusinessValidationException(
                    "Backup file '" + backupLog.getFileName() + "' is missing from the backups directory.");
        }

        DbConnectionInfo connectionInfo = resolveConnectionInfo();
        Path defaultsFile = null;
        try {
            defaultsFile = writeDefaultsExtraFile(connectionInfo.username(), connectionInfo.password());

            List<String> command = List.of(
                    backupProperties.getMysqlRestorePath(),
                    "--defaults-extra-file=" + defaultsFile.toAbsolutePath(),
                    "--host=" + connectionInfo.host(),
                    "--port=" + connectionInfo.port(),
                    connectionInfo.database());

            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .redirectInput(dumpFile.toFile());
            runProcess(processBuilder, "mysql");
        } catch (Exception ex) {
            log.error("Restore FAILED for backup id={} file={}, requested by '{}'",
                    backupLogId, backupLog.getFileName(), restoredBy, ex);
            throw ex instanceof BusinessValidationException businessValidationException
                    ? businessValidationException
                    : new BusinessValidationException("Restore failed: " + ex.getMessage());
        } finally {
            deleteQuietly(defaultsFile);
        }

        log.warn("Database RESTORED from backup id={} file={}, requested by '{}' at {}",
                backupLogId, backupLog.getFileName(), restoredBy, LocalDateTime.now());
    }

    @Override
    public List<BackupLogResponseDTO> listBackups() {
        return backupLogRepository.findAllByOrderByStartedAtDesc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public void deleteBackup(Long backupLogId) {
        BackupLog backupLog = backupLogRepository.findById(backupLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", backupLogId));

        Path file = Path.of(fileStorageProperties.getBackupPath())
                .resolve(backupLog.getFileName())
                .toAbsolutePath()
                .normalize();
        deleteQuietly(file);
        backupLogRepository.delete(backupLog);
    }

    // ---------------------------------------------------------------
    // retention
    // ---------------------------------------------------------------

    /**
     * Straightforward retention: any SUCCESS backup within {@code
     * retention-daily} days is always kept; beyond that, the earliest
     * SUCCESS backup of each calendar month is kept as long as that month
     * falls within {@code retention-monthly} months, everything else is
     * removed. Only ever called after a successful AUTOMATIC backup.
     */
    private void applyRetention() {
        List<BackupLog> successes = backupLogRepository.findByStatusOrderByStartedAtAsc(BackupStatus.SUCCESS);
        LocalDateTime dailyCutoff = LocalDateTime.now().minusDays(backupProperties.getRetentionDaily());
        LocalDateTime monthlyCutoff = LocalDateTime.now().minusMonths(backupProperties.getRetentionMonthly());
        Set<YearMonth> monthlyKept = new HashSet<>();

        for (BackupLog backupLog : successes) {
            if (backupLog.getStartedAt().isAfter(dailyCutoff)) {
                continue;
            }
            YearMonth month = YearMonth.from(backupLog.getStartedAt());
            boolean isFirstSeenInMonth = monthlyKept.add(month);
            boolean withinMonthlyWindow = backupLog.getStartedAt().isAfter(monthlyCutoff);
            if (isFirstSeenInMonth && withinMonthlyWindow) {
                continue;
            }

            Path file = Path.of(fileStorageProperties.getBackupPath())
                    .resolve(backupLog.getFileName())
                    .toAbsolutePath()
                    .normalize();
            deleteQuietly(file);
            backupLogRepository.delete(backupLog);
            log.info("Retention removed backup '{}' (started {})", backupLog.getFileName(), backupLog.getStartedAt());
        }
    }

    // ---------------------------------------------------------------
    // process execution
    // ---------------------------------------------------------------

    private void runProcess(ProcessBuilder processBuilder, String toolName) {
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException ex) {
            throw new BusinessValidationException(
                    "Could not start " + toolName + ": " + ex.getMessage());
        }

        String stderr;
        try (InputStream errorStream = process.getErrorStream()) {
            stderr = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            stderr = "";
        }

        boolean finished;
        try {
            finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BusinessValidationException(toolName + " was interrupted.");
        }

        if (!finished) {
            process.destroyForcibly();
            throw new BusinessValidationException(
                    toolName + " did not complete within " + PROCESS_TIMEOUT_MINUTES + " minutes and was terminated.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new BusinessValidationException(
                    toolName + " exited with code " + exitCode + (stderr.isBlank() ? "" : ": " + stderr.trim()));
        }
    }

    // ---------------------------------------------------------------
    // credentials / connection details
    // ---------------------------------------------------------------

    /**
     * Writes a MySQL {@code --defaults-extra-file} (the recommended way to
     * pass credentials to {@code mysqldump}/{@code mysql} without exposing
     * them on the command line, where any other process on the machine
     * could read them from a process listing). Restricted to owner-only
     * where the filesystem supports POSIX permissions; on Windows the temp
     * file relies on the OS temp directory already being user-scoped, and
     * is deleted immediately after use regardless.
     */
    private Path writeDefaultsExtraFile(String username, String password) throws IOException {
        Path file = Files.createTempFile("hfms-backup-", ".cnf");
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
        String content = "[client]\nuser=" + username + "\npassword=" + password + "\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private DbConnectionInfo resolveConnectionInfo() {
        String url = dataSourceProperties.getUrl();
        URI uri = URI.create(url.substring("jdbc:".length()));
        String database = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
        return new DbConnectionInfo(uri.getHost(), uri.getPort(), database,
                dataSourceProperties.getUsername(), dataSourceProperties.getPassword());
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Could not delete '{}': {}", path, ex.getMessage());
        }
    }

    private BackupLogResponseDTO toResponseDTO(BackupLog backupLog) {
        return BackupLogResponseDTO.builder()
                .id(backupLog.getId())
                .backupType(backupLog.getBackupType())
                .fileName(backupLog.getFileName())
                .fileSizeBytes(backupLog.getFileSizeBytes())
                .status(backupLog.getStatus())
                .errorMessage(backupLog.getErrorMessage())
                .startedAt(backupLog.getStartedAt())
                .completedAt(backupLog.getCompletedAt())
                .triggeredBy(backupLog.getTriggeredBy())
                .build();
    }

    private record DbConnectionInfo(String host, int port, String database, String username, String password) {
    }
}
