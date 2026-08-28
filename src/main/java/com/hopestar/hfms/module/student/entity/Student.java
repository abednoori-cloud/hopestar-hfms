package com.hopestar.hfms.module.student.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.module.auth.entity.Branch;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A student enrolled with the consultancy, per SRS Module 3.
 * <p>
 * Every student belongs to exactly one {@link Program} (the program they
 * are currently pursuing) but may accumulate multiple {@link
 * StudentContract} rows over time (re-enrollment, program change) and
 * multiple {@link StudentDocument} rows, per the approved business rules.
 * <p>
 * Students are never hard-deleted — only deactivated via {@link
 * BaseEntity#softDelete()} — per SRS §"CRUD" and the approved Business
 * Rules §4.
 */
@Getter
@Setter
@Entity
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(name = "uk_students_student_code", columnNames = "student_code"),
        @UniqueConstraint(name = "uk_students_passport_number", columnNames = "passport_number"),
        @UniqueConstraint(name = "uk_students_email", columnNames = "email")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"contracts", "documents"})
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class Student extends BaseEntity {

    /** System-generated, e.g. {@code STU-2026-000001}. Never user-editable. */
    @NotBlank
    @Size(max = 20)
    @Column(name = "student_code", nullable = false, length = 20, updatable = false)
    private String studentCode;

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Size(max = 150)
    @Column(name = "father_name", length = 150)
    private String fatherName;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Email
    @Size(max = 100)
    @Column(name = "email", length = 100)
    private String email;

    @Size(max = 30)
    @Column(name = "passport_number", length = 30)
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false, foreignKey = @ForeignKey(name = "fk_students_program"))
    private Program program;

    @Size(max = 100)
    @Column(name = "destination_country", length = 100)
    private String destinationCountry;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false, foreignKey = @ForeignKey(name = "fk_students_status"))
    private StudentStatus status;

    @NotNull
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "fk_students_branch"))
    private Branch branch;

    @Builder.Default
    @OneToMany(mappedBy = "student", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<StudentContract> contracts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "student", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<StudentDocument> documents = new ArrayList<>();
}
