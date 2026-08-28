package com.hopestar.hfms.module.finance.employee.entity;

/**
 * A fixed, small administrative classification — kept as a hardcoded
 * enum (backed by a {@code CHECK} constraint) rather than a lookup
 * table, consistent with {@code DocumentType}/{@code ContractStatus} in
 * the Student module.
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER
}
