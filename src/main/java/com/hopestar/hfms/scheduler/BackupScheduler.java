package com.hopestar.hfms.scheduler;

import com.hopestar.hfms.common.enums.BackupType;
import com.hopestar.hfms.common.util.SecurityUtil;
import com.hopestar.hfms.module.backup.repository.BackupScheduleRepository;
import com.hopestar.hfms.module.backup.service.BackupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Nightly automatic database backup, driven entirely by the {@code
 * backup_schedule_settings} row rather than a fixed {@code
 * @Scheduled(cron = "${...}")} annotation. That annotation-based approach
 * cannot change at runtime -- Spring resolves the cron placeholder exactly
 * once, at startup, and {@code @ConditionalOnProperty} decides once
 * whether the bean (and therefore any trigger) exists at all -- so an
 * admin changing the schedule through the UI would need a full
 * application restart to take effect. Instead, this class programmatically
 * schedules/reschedules the job on the {@link TaskScheduler} bean {@code
 * SchedulerConfig} already provisions, via {@link #applySchedule}, which
 * {@code BackupScheduleServiceImpl} calls immediately after persisting a
 * changed schedule -- so the change is live in the same request, in the
 * same running process, no restart involved.
 * <p>
 * {@link #currentTask} holds the in-flight {@link ScheduledFuture} (or
 * {@code null} when disabled) so it can be cancelled before a new one is
 * registered; guarded by an {@link AtomicReference} since startup and an
 * admin's live update could otherwise race.
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
public class BackupScheduler {

    private final TaskScheduler taskScheduler;
    private final BackupScheduleRepository backupScheduleRepository;
    private final BackupService backupService;

    private final AtomicReference<ScheduledFuture<?>> currentTask = new AtomicReference<>();

    /**
     * Applies whatever schedule is currently persisted, so the job is live
     * from the moment the application starts -- not just after the first
     * admin edit.
     */
    @PostConstruct
    void applyPersistedScheduleOnStartup() {
        backupScheduleRepository.findById(1L).ifPresentOrElse(
                schedule -> applySchedule(schedule.isAutoBackupEnabled(), schedule.getAutoBackupCron()),
                () -> log.warn("No backup_schedule_settings row found (expected id=1) -- automatic backup stays disabled."));
    }

    /**
     * Cancels whatever is currently scheduled (a no-op if nothing is) and,
     * if {@code enabled}, registers a new {@link CronTrigger} on the shared
     * {@link TaskScheduler}. Safe to call repeatedly -- each call fully
     * replaces the previous schedule.
     */
    public synchronized void applySchedule(boolean enabled, String cronExpression) {
        ScheduledFuture<?> previous = currentTask.getAndSet(null);
        if (previous != null) {
            previous.cancel(false);
        }

        if (!enabled) {
            log.info("Automatic backup schedule disabled.");
            return;
        }

        ScheduledFuture<?> scheduled = taskScheduler.schedule(this::runNightlyBackup, new CronTrigger(cronExpression));
        currentTask.set(scheduled);
        log.info("Automatic backup scheduled: cron='{}'", cronExpression);
    }

    private void runNightlyBackup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                SecurityUtil.SYSTEM_USER, null, List.of(new SimpleGrantedAuthority("SETTINGS_MANAGE"))));
        try {
            log.info("Starting automatic backup");
            backupService.createBackup(BackupType.AUTOMATIC, null);
            log.info("Automatic backup completed successfully");
        } catch (Exception ex) {
            // The failure is already recorded in backup_logs by
            // createBackup itself; this catch only prevents an unhandled
            // exception from being logged solely as a scheduler error with
            // none of that context, and stops it propagating to
            // SchedulerConfig's generic error handler.
            log.error("Automatic backup failed", ex);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
