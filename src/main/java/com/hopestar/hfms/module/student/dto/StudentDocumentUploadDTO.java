package com.hopestar.hfms.module.student.dto;

import com.hopestar.hfms.module.student.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Form metadata submitted alongside the uploaded file itself (the file
 * bytes travel as a separate {@code MultipartFile} controller parameter,
 * not as part of this DTO).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocumentUploadDTO {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    /** Optional — relevant for passports and similar expiring documents. */
    private LocalDate expiryDate;
}
