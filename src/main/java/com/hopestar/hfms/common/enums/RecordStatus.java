package com.hopestar.hfms.common.enums;

/**
 * Generic lifecycle status applied to master-data records that are not
 * financial transactions (e.g. can be toggled active/inactive by a user).
 * Financial/transactional statuses (DRAFT, POSTED, VOID, CANCELLED, etc.)
 * belong to their respective modules and are intentionally not modeled here.
 */
public enum RecordStatus {
    ACTIVE,
    INACTIVE
}
