package com.hopestar.hfms.module.finance.employee.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.employee.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-model for an employee. Serves both the list page and the detail/
 * view page — one response shape, per the same precedent {@code
 * StudentResponseDTO} set in the Student module, rather than separate
 * "List" and "View" DTOs with identical fields (which would be exactly
 * the kind of duplication this phase's instructions prohibit).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO extends BaseAuditDTO {

    private String employeeCode;
    private String fullName;
    private String fatherName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String nationalId;
    private String address;
    private String position;
    private String department;
    private LocalDate joiningDate;
    private EmployeeStatusResponseDTO employmentStatus;
    private BigDecimal baseSalary;
    private SupportedCurrency salaryCurrency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal usdEquivalentSalary;
    private String notes;
    private String branchName;
}
