package com.hopestar.hfms.config;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves the {@code mysqldump}/{@code mysql} executable path at startup
 * when the configured value is still a bare command name that doesn't
 * actually resolve on {@code PATH} -- the situation a fresh Windows install
 * lands in, since {@code install-mysql-headless.ps1} never adds MySQL's
 * {@code bin} directory to {@code PATH} (see {@link BackupConfig}).
 * <p>
 * Windows-only by design: on Linux/CI, {@code mysqldump}/{@code mysql} come
 * from the distro's {@code mysql-client} package and are reliably on
 * {@code PATH} already (see application.yml's bare-command default), so
 * this never runs there -- it exists purely for the packaged Windows
 * installer scenario. Any path that is already specific -- an explicit
 * {@code HFMS_MYSQLDUMP_PATH}/{@code HFMS_MYSQL_PATH} env var override, or
 * one the launcher derived from {@code MYSQL_BIN_DIR} in {@code
 * db.properties} -- is trusted as-is and never scanned.
 */
@Slf4j
final class MysqlToolLocator {

    private static final Path COMMON_MYSQL_ROOT = Path.of("C:\\Program Files\\MySQL");

    private MysqlToolLocator() {
    }

    static String resolve(String toolName, String configuredPath) {
        return resolve(toolName, configuredPath, COMMON_MYSQL_ROOT);
    }

    /**
     * Package-private overload taking the MySQL install root to scan, so
     * tests can point it at a synthetic directory instead of the real
     * {@code C:\Program Files\MySQL}.
     */
    static String resolve(String toolName, String configuredPath, Path mysqlRoot) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return configuredPath;
        }
        if (configuredPath.contains("/") || configuredPath.contains("\\")) {
            return configuredPath;
        }
        if (!isWindows() || isOnPath(configuredPath)) {
            return configuredPath;
        }

        Optional<Path> detected = scanCommonInstallLocations(toolName, mysqlRoot);
        if (detected.isPresent()) {
            log.info("'{}' not found on PATH; auto-detected at '{}'.", toolName, detected.get());
            return detected.get().toString();
        }
        return configuredPath;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static boolean isOnPath(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return false;
        }
        String exeName = command.toLowerCase().endsWith(".exe") ? command : command + ".exe";
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            Path candidate = Path.of(dir, exeName);
            if (Files.isExecutable(candidate) && !Files.isDirectory(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans {@code C:\Program Files\MySQL\MySQL Server *\bin} for {@code
     * <toolName>.exe} -- version-agnostic by design (no hardcoded "9.7" or
     * similar), since the installed MySQL version varies by machine. Only
     * one MySQL Server install is expected in this deployment model
     * (install-mysql-headless.ps1 manages exactly one), so the first match
     * is used rather than arbitrating between multiple versions.
     */
    private static Optional<Path> scanCommonInstallLocations(String toolName, Path mysqlRoot) {
        if (!Files.isDirectory(mysqlRoot)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> serverDirs = Files.newDirectoryStream(mysqlRoot, "MySQL Server *")) {
            for (Path serverDir : serverDirs) {
                Path candidate = serverDir.resolve("bin").resolve(toolName + ".exe");
                if (Files.isExecutable(candidate)) {
                    return Optional.of(candidate);
                }
            }
        } catch (IOException ex) {
            log.warn("Error scanning '{}' for a MySQL install: {}", mysqlRoot, ex.getMessage());
        }
        return Optional.empty();
    }
}
