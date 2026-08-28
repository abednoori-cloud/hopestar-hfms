package com.hopestar.hfms.module.student.dto;

import com.hopestar.hfms.common.dto.BaseAuditDTO;
import com.hopestar.hfms.module.student.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocumentResponseDTO extends BaseAuditDTO {

    private Long studentId;
    private DocumentType documentType;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
    private LocalDate expiryDate;
    private boolean expiringSoon;

    /** Relative download URL for the document, built by the controller/service. */
    private String downloadUrl;
}
