package com.hopestar.hfms.scheduler;

import com.hopestar.hfms.common.enums.BackupType;
import com.hopestar.hfms.common.util.SecurityUtil;
import com.hopestar.hfms.module.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly automatic database backup, per {@code hfms.backup.auto-backup-cron}
 * (Phase 1 default: 02:00 daily). Uses the {@link org.springframework.scheduling.TaskScheduler}
 * bean already provisioned by {@code SchedulerConfig} -- no second scheduler
 * is created here.
 * <p>
 * {@code @ConditionalOnProperty} means this bean (and therefore its {@code
 * @Scheduled} trigger) does not exist at all when {@code
 * hfms.backup.auto-backup-enabled=false}, which is dev's default -- rather
 * than registering the job and no-op-ing inside it every night.
 * <p>
 * {@link BackupService#createBackup} is {@code @PreAuthorize}-gated on
 * {@code SETTINGS_MANAGE} since it is also reachable from the "Backup Now"
 * button; a scheduled job has no logged-in user, so a short-lived system
 * {@link org.springframework.security.core.Authentication} carrying that
 * authority is installed for the duration of this call only, then cleared.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hfms.backup", name = "auto-backup-enabled", havingValue = "true")
public class BackupScheduler {

    private final BackupService backupService;

    @Scheduled(cron = "${hfms.backup.auto-backup-cron}")
    public void runNightlyBackup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                SecurityUtil.SYSTEM_USER, null, List.of(new SimpleGrantedAuthority("SETTINGS_MANAGE"))));
        try {
            log.info("Starting nightly automatic backup");
            backupService.createBackup(BackupType.AUTOMATIC, null);
            log.info("Nightly automatic backup completed successfully");
        } catch (Exception ex) {
            // The failure is already recorded in backup_logs by
            // createBackup itself; this catch only prevents an unhandled
            // exception from being logged solely as a scheduler error with
            // none of that context, and stops it propagating to
            // SchedulerConfig's generic error handler.
            log.error("Nightly automatic backup failed", ex);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
