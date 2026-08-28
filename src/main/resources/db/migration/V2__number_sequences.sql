-- =====================================================================
-- V2__number_sequences.sql
-- Shared, atomic business-key generator backing table -- see approved
-- architecture's Invoice Numbering Strategy (§6). Introduced now because
-- Student Management (V3) is the first module that needs it (student
-- codes); the same table/mechanism will be reused unmodified for
-- employee codes, invoice numbers, and transaction codes in later phases.
-- =====================================================================

CREATE TABLE number_sequences (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequence_key      VARCHAR(30)  NOT NULL,
    -- 0 = "not year-scoped" (e.g. employee codes); a real year value
    -- (e.g. 2026) for sequences that reset annually (e.g. student codes).
    -- Kept NOT NULL with a sentinel rather than nullable: MySQL treats
    -- NULL as distinct in unique indexes, which would silently defeat
    -- the uniqueness guarantee below for any row using NULL.
    sequence_year     INT          NOT NULL DEFAULT 0,
    -- Always a real branches.id (defaults to the headquarters branch in
    -- today's single-branch deployment) for the same reason.
    branch_id         BIGINT       NOT NULL,
    prefix            VARCHAR(10)  NOT NULL,
    padding_length    INT          NOT NULL,
    sequence_last_value BIGINT       NOT NULL DEFAULT 0,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_number_sequences_key_year_branch
        UNIQUE (sequence_key, sequence_year, branch_id),
    CONSTRAINT fk_number_sequences_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
