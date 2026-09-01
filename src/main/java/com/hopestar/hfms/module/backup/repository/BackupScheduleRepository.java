package com.hopestar.hfms.module.backup.repository;

import com.hopestar.hfms.module.backup.entity.BackupSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

/** Always exactly one row, id 1, seeded by {@code V16__backup_schedule_settings.sql}. */
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {
}
