package com.hopestar.hfms.module.finance.employee.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.employee.entity.Gender;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input for updating an existing employee. {@code employeeCode} remains
 * absent/immutable, mirroring {@code StudentUpdateDTO}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 150)
    private String fatherName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String phone;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100)
    private String email;

    @Size(max = 50)
    private String nationalId;

    @Size(max = 255)
    private String address;

    @NotBlank(message = "Position is required")
    @Size(max = 100)
    private String position;

    @NotBlank(message = "Department is required")
    @Size(max = 100)
    private String department;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @NotNull(message = "Employment status is required")
    private Long employmentStatusId;

    @NotNull(message = "Base salary is required")
    @DecimalMin(value = "0.0", message = "Base salary cannot be negative")
    private BigDecimal baseSalary;

    @NotNull(message = "Salary currency is required")
    private SupportedCurrency salaryCurrency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRateToUsd;

    @Size(max = 2000)
    private String notes;

    private Long branchId;
}
