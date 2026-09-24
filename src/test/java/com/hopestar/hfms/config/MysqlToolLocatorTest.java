package com.hopestar.hfms.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MysqlToolLocatorTest {

    @Test
    void leavesAnAlreadySpecificPathUntouched(@TempDir Path tempDir) {
        String explicitPath = tempDir.resolve("mysqldump.exe").toString();

        String resolved = MysqlToolLocator.resolve("mysqldump", explicitPath, tempDir);

        assertThat(resolved).isEqualTo(explicitPath);
    }

    @Test
    void leavesABlankOrNullPathUntouched(@TempDir Path tempDir) {
        assertThat(MysqlToolLocator.resolve("mysqldump", "", tempDir)).isEmpty();
        assertThat(MysqlToolLocator.resolve("mysqldump", null, tempDir)).isNull();
    }

    @Test
    void findsMysqldumpUnderASyntheticMysqlServerInstall(@TempDir Path mysqlRoot) throws IOException {
        assumeTrue(isWindows(), "MysqlToolLocator's scan is Windows-only by design");

        Path binDir = Files.createDirectories(mysqlRoot.resolve("MySQL Server 26.7").resolve("bin"));
        Path expected = Files.createFile(binDir.resolve("mysqldump.exe"));

        String resolved = MysqlToolLocator.resolve("mysqldump", "mysqldump-test-tool-does-not-exist", mysqlRoot);

        assertThat(resolved).isEqualTo(expected.toString());
    }

    @Test
    void findsMysqlUnderASyntheticMysqlServerInstall(@TempDir Path mysqlRoot) throws IOException {
        assumeTrue(isWindows(), "MysqlToolLocator's scan is Windows-only by design");

        Path binDir = Files.createDirectories(mysqlRoot.resolve("MySQL Server 26.7").resolve("bin"));
        Path expected = Files.createFile(binDir.resolve("mysql.exe"));

        String resolved = MysqlToolLocator.resolve("mysql", "mysql-test-tool-does-not-exist", mysqlRoot);

        assertThat(resolved).isEqualTo(expected.toString());
    }

    @Test
    void fallsBackToTheConfiguredNameWhenNothingIsFound(@TempDir Path emptyMysqlRoot) {
        String resolved = MysqlToolLocator.resolve(
                "mysqldump", "mysqldump-test-tool-does-not-exist", emptyMysqlRoot);

        assertThat(resolved).isEqualTo("mysqldump-test-tool-does-not-exist");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
