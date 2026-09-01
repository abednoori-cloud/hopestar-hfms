package com.hopestar.hfms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code hfms.backup.*} keys from application.yml. The
 * auto-backup-enabled/auto-backup-cron keys were already present in Phase
 * 1's application.yml but unbound to any Java class until this module;
 * {@link #mysqldumpPath}/{@link #mysqlRestorePath} are new.
 * <p>
 * There is deliberately no automatic-retention/cleanup setting here: per
 * explicit product decision, the system never deletes a backup file or
 * its {@code backup_logs} row on its own -- the only removal path is the
 * user-triggered {@code BackupService.deleteBackup}. An earlier revision
 * of this class had {@code retentionDaily}/{@code retentionMonthly}
 * fields backing an automatic-cleanup feature; both the fields and the
 * feature were removed together, not left behind as inert config -- see
 * the Phase 1 postmortem on the old Invoice scaffolding for why a config
 * key with nothing reading it is worse than no config key at all.
 * <p>
 * Neither {@code mysqldump} nor {@code mysql} is guaranteed to be on
 * {@code PATH}, so these are explicit, environment-overridable absolute
 * paths rather than bare command names -- see application-dev.yml for
 * this machine's MySQL Server 9.7 install location.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "hfms.backup")
public class BackupProperties {

    /** Master switch for the nightly scheduled backup job. */
    private boolean autoBackupEnabled = true;

    /** Cron expression (Spring's 6-field form) for the nightly backup job. */
    private String autoBackupCron = "0 0 2 * * *";

    /** Absolute path to the {@code mysqldump} executable used for backups. */
    private String mysqldumpPath = "mysqldump";

    /** Absolute path to the {@code mysql} client executable used for restores. */
    private String mysqlRestorePath = "mysql";
}
