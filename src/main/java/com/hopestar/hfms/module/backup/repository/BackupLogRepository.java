package com.hopestar.hfms.module.backup.repository;

import com.hopestar.hfms.common.enums.BackupStatus;
import com.hopestar.hfms.module.backup.entity.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupLogRepository extends JpaRepository<BackupLog, Long> {

    List<BackupLog> findAllByOrderByStartedAtDesc();

    /** Ascending so retention cleanup sees each month's earliest backup first. */
    List<BackupLog> findByStatusOrderByStartedAtAsc(BackupStatus status);
}
