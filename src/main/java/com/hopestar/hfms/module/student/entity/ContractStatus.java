package com.hopestar.hfms.module.student.entity;

/**
 * Lifecycle status of a {@link StudentContract}. Backed by a
 * {@code CHECK} constraint on {@code student_contracts.status} rather
 * than a lookup table, consistent with {@link DocumentType}.
 */
public enum ContractStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
