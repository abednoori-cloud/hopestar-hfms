package com.hopestar.hfms.common.enums;

/**
 * Action types recorded in {@code audit_logs}, written by
 * {@code com.hopestar.hfms.audit.listener.AuditEntityListener} whenever
 * an audited entity is created, updated, soft-deleted or voided.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    VOID
}
