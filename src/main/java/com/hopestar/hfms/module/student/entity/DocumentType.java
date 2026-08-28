package com.hopestar.hfms.module.student.entity;

/**
 * Supported student document categories, per SRS Module 3. Backed by a
 * {@code CHECK} constraint on {@code student_documents.document_type}
 * rather than a lookup table — this list is fixed by the kind of
 * documentation a study-abroad consultancy collects and is not expected
 * to change at runtime the way, say, expense categories might.
 */
public enum DocumentType {
    PASSPORT,
    DIPLOMA,
    TRANSCRIPT,
    ENGLISH_CERTIFICATE,
    CONTRACT,
    OTHER
}
