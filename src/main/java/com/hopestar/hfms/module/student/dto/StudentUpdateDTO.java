package com.hopestar.hfms.module.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Input for updating an existing student. {@code studentCode} is still
 * absent — it is immutable once generated, per the approved business
 * rules; this DTO edits every other field.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentUpdateDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 150)
    private String fatherName;

    @Size(max = 20)
    private String phone;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100)
    private String email;

    @Size(max = 30)
    private String passportNumber;

    @FutureOrPresent(message = "Passport expiry cannot be in the past")
    private LocalDate passportExpiry;

    @NotNull(message = "Program is required")
    private Long programId;

    @Size(max = 100)
    private String destinationCountry;

    @NotNull(message = "Status is required")
    private Long statusId;

    @NotNull(message = "Registration date is required")
    @PastOrPresent(message = "Registration date cannot be in the future")
    private LocalDate registrationDate;

    @Size(max = 2000)
    private String notes;

    private Long branchId;
}
