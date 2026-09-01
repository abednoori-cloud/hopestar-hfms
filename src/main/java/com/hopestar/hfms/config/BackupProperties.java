package com.hopestar.hfms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code hfms.backup.*} keys from application.yml -- now just
 * the {@code mysqldump}/{@code mysql} executable paths. {@code
 * auto-backup-enabled}/{@code auto-backup-cron} used to live here too, but
 * moved to the {@code backup_schedule_settings} table (see {@code
 * BackupSchedule}): a static YAML file can't be safely edited by a
 * running application, and even if it could, {@code @Scheduled(cron =
 * "${...}")} only ever resolves that placeholder once at startup -- an
 * admin-editable schedule has to be real, persisted data that {@code
 * BackupScheduler} can reschedule live via the existing {@code
 * TaskScheduler} bean.
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
 * this machine's MySQL Server 9.7 install location. Unlike the schedule,
 * these deliberately stay in {@code application.yml}: they are machine/
 * environment-specific install paths, an ops concern, not something an
 * office admin should edit through a web form.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "hfms.backup")
public class BackupProperties {

    /** Absolute path to the {@code mysqldump} executable used for backups. */
    private String mysqldumpPath = "mysqldump";

    /** Absolute path to the {@code mysql} client executable used for restores. */
    private String mysqlRestorePath = "mysql";
}
