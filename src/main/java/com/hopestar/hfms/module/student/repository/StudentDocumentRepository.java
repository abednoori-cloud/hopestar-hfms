package com.hopestar.hfms.module.student.repository;

import com.hopestar.hfms.module.student.entity.DocumentType;
import com.hopestar.hfms.module.student.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudentIdAndActiveTrueOrderByUploadedAtDesc(Long studentId);

    Optional<StudentDocument> findByIdAndActiveTrue(Long id);

    List<StudentDocument> findByStudentIdAndDocumentTypeAndActiveTrue(Long studentId, DocumentType documentType);

    List<StudentDocument> findByExpiryDateBetweenAndActiveTrue(LocalDate from, LocalDate to);

    long countByStudentIdAndActiveTrue(Long studentId);
}
