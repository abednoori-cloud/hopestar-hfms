package com.hopestar.hfms.module.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/** Read-model for the current automatic-backup schedule. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupScheduleResponseDTO {

    private boolean enabled;

    /** The underlying cron expression, for transparency -- the UI itself only edits {@link #time}. */
    private String cronExpression;

    /** {@link #cronExpression} parsed back into a plain daily time, for the time-picker's default value. */
    private LocalTime time;
}
