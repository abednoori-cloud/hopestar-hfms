package com.hopestar.hfms.module.student.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Full read-model for a student, including nested program/status
 * summaries so list/detail views need no additional round trips.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDTO extends BaseAuditDTO {

    private String studentCode;
    private String fullName;
    private String fatherName;
    private String phone;
    private String email;
    private String passportNumber;
    private LocalDate passportExpiry;
    private ProgramResponseDTO program;
    private String destinationCountry;
    private StudentStatusResponseDTO status;
    private LocalDate registrationDate;
    private String notes;
    private String branchName;
    private int contractCount;
    private int documentCount;
}
