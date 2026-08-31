package com.hopestar.hfms.common.service;

/**
 * Generates unique, gap-free, human-readable business keys (student codes,
 * employee codes, receipt numbers, transaction codes) per the approved
 * Numbering Strategy (§6): one shared mechanism instead of a bespoke
 * counter per module.
 */
public interface SequenceGeneratorService {

    /**
     * Returns the next formatted value for a sequence that resets every
     * calendar year (e.g. student codes: {@code STU-2026-000001}).
     *
     * @param sequenceKey   logical counter name, e.g. {@code "STUDENT"}
     * @param prefix        printed prefix, e.g. {@code "STU"}
     * @param paddingLength zero-padding width for the numeric part
     * @param branchId      branch to scope the counter to, or {@code null}
     *                      for a single global counter
     */
    String nextYearlyValue(String sequenceKey, String prefix, int paddingLength, Long branchId);

    /**
     * Returns the next formatted value for a sequence that never resets
     * (e.g. employee codes: {@code EMP-000001}).
     */
    String nextPerpetualValue(String sequenceKey, String prefix, int paddingLength, Long branchId);
}
