package com.hopestar.hfms.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Activates {@link BackupProperties}. Unlike {@link FileStorageConfig}'s
 * directory check (essential infrastructure the app cannot run without),
 * a missing {@code mysqldump}/{@code mysql} executable only breaks the
 * Backup & Restore feature itself, not the rest of the app -- so this
 * logs a startup warning rather than failing the whole application to
 * start, which matters most in dev where the feature may be exercised
 * rarely.
 * <p>
 * Before warning, first runs each configured path through {@link
 * MysqlToolLocator}, which -- on Windows only -- auto-detects the real
 * install location when the configured value is still a bare command name
 * that isn't actually on {@code PATH}. This is what lets an existing
 * packaged install self-heal from an app update alone, without needing
 * {@code install-mysql-headless.ps1} to be rerun (its {@code
 * MYSQL_BIN_DIR} write into {@code db.properties} only benefits fresh
 * installs going forward).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BackupProperties.class)
@RequiredArgsConstructor
public class BackupConfig {

    private final BackupProperties backupProperties;

    @PostConstruct
    public void checkExecutables() {
        backupProperties.setMysqldumpPath(
                MysqlToolLocator.resolve("mysqldump", backupProperties.getMysqldumpPath()));
        backupProperties.setMysqlRestorePath(
                MysqlToolLocator.resolve("mysql", backupProperties.getMysqlRestorePath()));

        warnIfMissing("mysqldump", backupProperties.getMysqldumpPath());
        warnIfMissing("mysql", backupProperties.getMysqlRestorePath());
    }

    private void warnIfMissing(String toolName, String configuredPath) {
        // A bare command name (no path separator) is assumed to resolve via
        // PATH at execution time -- not something we can verify here.
        if (!configuredPath.contains("/") && !configuredPath.contains("\\")) {
            return;
        }
        Path path = Path.of(configuredPath);
        if (!Files.isExecutable(path)) {
            log.warn("Configured {} executable not found or not executable at '{}' -- "
                            + "Backup & Restore will fail until hfms.backup.{}-path is corrected.",
                    toolName, configuredPath, toolName.equals("mysql") ? "mysql-restore" : "mysqldump");
        }
    }
}
