package com.hopestar.hfms.module.backup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

/**
 * Input for the Backup Schedule form: a plain on/off switch and a daily
 * time -- no raw cron expression is ever exposed to the UI, so there is
 * nothing for an admin to type incorrectly. {@code BackupScheduleServiceImpl}
 * converts {@link #time} to Spring's 6-field cron form internally.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BackupScheduleUpdateDTO {

    private boolean enabled;

    @NotNull(message = "A time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime time;
}
