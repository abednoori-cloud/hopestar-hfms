package com.hopestar.hfms.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/** Dashboard's key student/employee statistics widget. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEmployeeStatsDTO {

    private long activeStudentCount;
    private long activeEmployeeCount;

    /** Active student count grouped by status name (e.g. ACTIVE, ON_HOLD, COMPLETED). */
    private Map<String, Long> activeStudentsByStatus;
}
