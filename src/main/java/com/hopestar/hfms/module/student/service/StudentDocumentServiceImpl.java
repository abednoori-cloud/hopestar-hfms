package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.common.dto.StoredFileInfo;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.FileStorageService;
import com.hopestar.hfms.common.util.DateUtil;
import com.hopestar.hfms.module.student.dto.StudentDocumentResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentDocumentUploadDTO;
import com.hopestar.hfms.module.student.entity.Student;
import com.hopestar.hfms.module.student.entity.StudentDocument;
import com.hopestar.hfms.module.student.repository.StudentDocumentRepository;
import com.hopestar.hfms.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentDocumentServiceImpl implements StudentDocumentService {

    /** A passport/certificate is flagged "expiring soon" within this many days. */
    private static final int EXPIRY_WARNING_WINDOW_DAYS = 30;

    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public StudentDocumentResponseDTO upload(Long studentId, StudentDocumentUploadDTO metadata, MultipartFile file) {
        Student student = studentRepository.findByIdAndActiveTrue(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        StoredFileInfo storedFile = fileStorageService.storeDocument(file, "students/" + studentId);

        StudentDocument document = StudentDocument.builder()
                .student(student)
                .documentType(metadata.getDocumentType())
                .filePath(storedFile.getStoredPath())
                .originalFileName(storedFile.getOriginalFileName())
                .contentType(storedFile.getContentType())
                .fileSizeBytes(storedFile.getSizeBytes())
                .uploadedAt(LocalDateTime.now())
                .expiryDate(metadata.getExpiryDate())
                .build();

        StudentDocument saved = studentDocumentRepository.save(document);
        return toResponseDTO(saved);
    }

    @Override
    public StudentDocumentResponseDTO getById(Long documentId) {
        return toResponseDTO(getActiveDocumentEntity(documentId));
    }

    @Override
    public List<StudentDocumentResponseDTO> listByStudent(Long studentId) {
        return studentDocumentRepository.findByStudentIdAndActiveTrueOrderByUploadedAtDesc(studentId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deactivate(Long documentId) {
        StudentDocument document = getActiveDocumentEntity(documentId);
        // Physical file is intentionally left on disk -- see this
        // service's Javadoc and StudentDocument's class Javadoc for why.
        document.softDelete();
    }

    @Override
    public String resolveFilePath(Long documentId) {
        return getActiveDocumentEntity(documentId).getFilePath();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private StudentDocument getActiveDocumentEntity(Long documentId) {
        return studentDocumentRepository.findByIdAndActiveTrue(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student document", documentId));
    }

    private StudentDocumentResponseDTO toResponseDTO(StudentDocument document) {
        return StudentDocumentResponseDTO.builder()
                .id(document.getId())
                .active(document.isActive())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .createdBy(document.getCreatedBy())
                .updatedBy(document.getUpdatedBy())
                .studentId(document.getStudent().getId())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .fileSizeBytes(document.getFileSizeBytes())
                .uploadedAt(document.getUploadedAt())
                .expiryDate(document.getExpiryDate())
                .expiringSoon(DateUtil.isWithinDays(document.getExpiryDate(), EXPIRY_WARNING_WINDOW_DAYS))
                .downloadUrl("/students/" + document.getStudent().getId() + "/documents/" + document.getId() + "/download")
                .build();
    }
}
