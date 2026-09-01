package com.hopestar.hfms.module.backup.service;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.module.backup.dto.BackupScheduleResponseDTO;
import com.hopestar.hfms.module.backup.dto.BackupScheduleUpdateDTO;
import com.hopestar.hfms.module.backup.entity.BackupSchedule;
import com.hopestar.hfms.module.backup.repository.BackupScheduleRepository;
import com.hopestar.hfms.scheduler.BackupScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/**
 * Implements {@link BackupScheduleService}. {@link #updateSchedule} both
 * persists the change and calls {@link BackupScheduler#applySchedule} in
 * the same method -- the whole point of moving the schedule into the
 * database is that an admin's change takes effect immediately, not just
 * after the next restart.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackupScheduleServiceImpl implements BackupScheduleService {

    /** {@code id} of the single row seeded by V16__backup_schedule_settings.sql. */
    private static final long SCHEDULE_ROW_ID = 1L;

    private final BackupScheduleRepository backupScheduleRepository;
    private final BackupScheduler backupScheduler;

    @Override
    public BackupScheduleResponseDTO getSchedule() {
        return toResponseDTO(getScheduleEntity());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public BackupScheduleResponseDTO updateSchedule(BackupScheduleUpdateDTO dto) {
        BackupSchedule schedule = getScheduleEntity();

        String cronExpression = toCron(dto.getTime());
        schedule.setAutoBackupEnabled(dto.isEnabled());
        schedule.setAutoBackupCron(cronExpression);

        backupScheduler.applySchedule(dto.isEnabled(), cronExpression);

        return toResponseDTO(schedule);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private BackupSchedule getScheduleEntity() {
        return backupScheduleRepository.findById(SCHEDULE_ROW_ID)
                .orElseThrow(() -> new BusinessValidationException("Backup schedule settings are not configured."));
    }

    /** Spring's 6-field cron form for "every day at this time". */
    private String toCron(LocalTime time) {
        return "0 %d %d * * *".formatted(time.getMinute(), time.getHour());
    }

    private LocalTime fromCron(String cronExpression) {
        String[] fields = cronExpression.trim().split("\\s+");
        int minute = Integer.parseInt(fields[1]);
        int hour = Integer.parseInt(fields[2]);
        return LocalTime.of(hour, minute);
    }

    private BackupScheduleResponseDTO toResponseDTO(BackupSchedule schedule) {
        return BackupScheduleResponseDTO.builder()
                .enabled(schedule.isAutoBackupEnabled())
                .cronExpression(schedule.getAutoBackupCron())
                .time(fromCron(schedule.getAutoBackupCron()))
                .build();
    }
}
