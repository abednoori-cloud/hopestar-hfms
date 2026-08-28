package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.module.student.dto.StudentDocumentResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentDocumentUploadDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Business operations for {@link com.hopestar.hfms.module.student.entity.StudentDocument}.
 * Every upload is validated and persisted through {@code FileStorageService}
 * (established in Phase 1), never written to disk directly by this
 * service. A student may have multiple documents, per the approved
 * business rules; documents are soft-deleted like every other entity, the
 * underlying file is left on disk for recovery.
 */
public interface StudentDocumentService {

    StudentDocumentResponseDTO upload(Long studentId, StudentDocumentUploadDTO metadata, MultipartFile file);

    StudentDocumentResponseDTO getById(Long documentId);

    List<StudentDocumentResponseDTO> listByStudent(Long studentId);

    void deactivate(Long documentId);

    /** Resolves the on-disk path for a document, for the download endpoint. */
    String resolveFilePath(Long documentId);
}
