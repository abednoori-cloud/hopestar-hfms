package com.hopestar.hfms.module.student.entity;

import com.hopestar.hfms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single uploaded document (Passport, Diploma, Transcript, English
 * Certificate, Contract, Other) attached to a {@link Student}, per SRS
 * Module 3. The physical file is written by {@link
 * com.hopestar.hfms.common.service.FileStorageService} (established in
 * Phase 1) — this entity stores only the resulting metadata, never file
 * bytes.
 * <p>
 * Soft-deleted like every other entity extending {@link BaseEntity}; the
 * physical file is intentionally left in place on soft-delete so a
 * mistakenly deactivated document can be recovered without needing to
 * re-upload.
 */
@Getter
@Setter
@Entity
@Table(name = "student_documents")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "student")
@EntityListeners(com.hopestar.hfms.audit.listener.AuditEntityListener.class)
public class StudentDocument extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_documents_student"))
    private Student student;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @NotBlank
    @Size(max = 500)
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Size(max = 255)
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Size(max = 100)
    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @NotNull
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
