package com.hopestar.hfms.module.backup.service;

import com.hopestar.hfms.module.backup.dto.BackupScheduleResponseDTO;
import com.hopestar.hfms.module.backup.dto.BackupScheduleUpdateDTO;

/**
 * Reads and updates the single {@code backup_schedule_settings} row. An
 * update takes effect immediately, in the same running process -- see
 * {@code BackupScheduler#applySchedule} -- not just on the next restart.
 */
public interface BackupScheduleService {

    BackupScheduleResponseDTO getSchedule();

    /**
     * Persists the new schedule and immediately reschedules (or stops)
     * the live automatic backup job to match.
     */
    BackupScheduleResponseDTO updateSchedule(BackupScheduleUpdateDTO dto);
}
