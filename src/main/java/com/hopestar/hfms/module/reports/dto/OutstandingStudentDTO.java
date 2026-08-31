package com.hopestar.hfms.module.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One row of the Student Report's "currently outstanding, all time" table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingStudentDTO {

    private Long contractId;
    private Long studentId;
    private String studentCode;
    private String studentFullName;
    private String programName;
    private BigDecimal contractUsdEquivalentAmount;
    private BigDecimal remainingBalanceUsd;
}
