package com.hopestar.hfms.module.backup.dto;

import com.hopestar.hfms.common.enums.BackupStatus;
import com.hopestar.hfms.common.enums.BackupType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Read-model for a {@code backup_logs} row, shown on the Backup & Restore page. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupLogResponseDTO {

    private Long id;
    private BackupType backupType;
    private String fileName;
    private Long fileSizeBytes;
    private BackupStatus status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String triggeredBy;
}
