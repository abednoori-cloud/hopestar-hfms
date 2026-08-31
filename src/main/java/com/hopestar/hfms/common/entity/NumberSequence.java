package com.hopestar.hfms.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Backs the atomic, gap-free business-key generator described in the
 * approved architecture's Numbering Strategy (§6): one shared mechanism,
 * reused for student codes, employee codes, receipt numbers, and
 * transaction codes, rather than a bespoke counter per module.
 * <p>
 * A row is keyed by {@code sequenceKey} (e.g. {@code "STUDENT"},
 * {@code "STUDENT_PAYMENT_RECEIPT"}), optionally scoped by {@code year}
 * (for sequences that reset annually) and by {@code branchId} (so
 * Multi-branch Support, once enabled, does not require renumbering
 * anything — see approved architecture §8). {@link
 * com.hopestar.hfms.common.service.SequenceGeneratorServiceImpl} increments
 * {@code lastValue} under a pessimistic row lock so concurrent requests can
 * never receive the same number.
 * <p>
 * Deliberately does not extend {@link BaseEntity}: this is internal
 * system-generator state, not a business/audit record.
 */
@Getter
@Setter
@Entity
@Table(name = "number_sequences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_number_sequences_key_year_branch",
                columnNames = {"sequence_key", "sequence_year", "branch_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** e.g. STUDENT, EMPLOYEE, TRANSACTION. */
    @Column(name = "sequence_key", nullable = false, length = 30)
    private String sequenceKey;

    /**
     * The calendar year this counter applies to, for sequences that reset
     * yearly (e.g. student codes, receipt numbers). {@code 0} is the
     * sentinel for "not year-scoped" (e.g. employee codes) -- kept
     * non-null because MySQL treats NULL as distinct in unique indexes,
     * which would defeat the uniqueness guarantee on this table.
     */
    @Column(name = "sequence_year", nullable = false)
    private int sequenceYear;

    /**
     * Branch this counter is scoped to (always a real {@code branches.id}
     * -- defaults to the headquarters branch in today's single-branch
     * deployment). Non-null for the same reason as {@link #sequenceYear};
     * this lets Multi-branch Support run independent counters per branch
     * later without a schema change.
     */
    @Column(name = "branch_id", nullable = false)
    private long branchId;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix;

    @Column(name = "padding_length", nullable = false)
    private int paddingLength;

    @Column(name = "\"last_value\"", nullable = false)
    private long lastValue;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
