-- =====================================================================
-- V3__student_management.sql
-- Student Management Module (SRS Module 3) -- approved architecture §2.2.
--
-- Currency is stored as a 3-letter ISO code (VARCHAR(3)) on programs and
-- student_contracts rather than a currency_id FK to a currencies table:
-- the full Currency entity is owned by the Finance ledger module
-- (approved architecture §2.4, package finance.ledger), which has not
-- been implemented yet. Student Management only needs to *record* which
-- currency a contract is priced in today; the currency master table and
-- FX-rate handling land with the Finance module without requiring a
-- migration of this data (a currency_id column can be added and backfilled
-- from currency_code at that point).
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- student_statuses (lookup)
-- ---------------------------------------------------------------------
CREATE TABLE student_statuses (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(50)  NOT NULL,
    description       VARCHAR(255) NULL,

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NULL,
    created_by        VARCHAR(50)  NULL,
    updated_by        VARCHAR(50)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_student_statuses_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- programs (master price list)
-- ---------------------------------------------------------------------
CREATE TABLE programs (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(150)   NOT NULL,
    destination_country   VARCHAR(100)   NOT NULL,
    base_price            DECIMAL(12,2)  NOT NULL,
    currency_code         VARCHAR(3)     NOT NULL DEFAULT 'USD',
    description           VARCHAR(500)   NULL,

    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at            DATETIME       NULL,
    created_at            DATETIME       NOT NULL,
    updated_at            DATETIME       NULL,
    created_by            VARCHAR(50)    NULL,
    updated_by            VARCHAR(50)    NULL,
    version               BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_programs_name_country UNIQUE (name, destination_country),
    CONSTRAINT chk_programs_base_price_non_negative CHECK (base_price >= 0),
    CONSTRAINT chk_programs_currency_code CHECK (currency_code IN ('USD', 'EUR', 'AFN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_programs_destination_country ON programs (destination_country);

-- ---------------------------------------------------------------------
-- students
-- ---------------------------------------------------------------------
CREATE TABLE students (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code          VARCHAR(20)    NOT NULL,
    full_name             VARCHAR(150)   NOT NULL,
    father_name           VARCHAR(150)   NULL,
    phone                 VARCHAR(20)    NULL,
    email                 VARCHAR(100)   NULL,
    passport_number       VARCHAR(30)    NULL,
    passport_expiry       DATE           NULL,
    program_id            BIGINT         NOT NULL,
    destination_country   VARCHAR(100)   NULL,
    status_id             BIGINT         NOT NULL,
    registration_date     DATE           NOT NULL,
    notes                 VARCHAR(2000)  NULL,
    branch_id             BIGINT         NULL,

    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at            DATETIME       NULL,
    created_at            DATETIME       NOT NULL,
    updated_at            DATETIME       NULL,
    created_by            VARCHAR(50)    NULL,
    updated_by            VARCHAR(50)    NULL,
    version               BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_students_student_code UNIQUE (student_code),
    -- NULL values do not collide under MySQL's unique-index semantics,
    -- so passport/email stay optional while still being unique whenever
    -- a value IS provided, per the approved business rules.
    CONSTRAINT uk_students_passport_number UNIQUE (passport_number),
    CONSTRAINT uk_students_email UNIQUE (email),
    CONSTRAINT fk_students_program
        FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_students_status
        FOREIGN KEY (status_id) REFERENCES student_statuses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_students_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_students_program_id ON students (program_id);
CREATE INDEX idx_students_status_id ON students (status_id);
CREATE INDEX idx_students_branch_id ON students (branch_id);
CREATE INDEX idx_students_full_name ON students (full_name);
CREATE INDEX idx_students_phone ON students (phone);
CREATE INDEX idx_students_destination_country ON students (destination_country);

-- ---------------------------------------------------------------------
-- student_contracts
-- ---------------------------------------------------------------------
CREATE TABLE student_contracts (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id               BIGINT         NOT NULL,
    program_id               BIGINT         NOT NULL,
    total_contract_amount    DECIMAL(12,2)  NOT NULL,
    registration_fee         DECIMAL(12,2)  NOT NULL DEFAULT 0,
    discount_amount          DECIMAL(12,2)  NOT NULL DEFAULT 0,
    discount_reason          VARCHAR(255)   NULL,
    final_amount             DECIMAL(12,2)  NOT NULL,
    currency_code            VARCHAR(3)     NOT NULL DEFAULT 'USD',
    contract_date            DATE           NOT NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    notes                    VARCHAR(2000)  NULL,

    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at               DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NULL,
    created_by               VARCHAR(50)    NULL,
    updated_by                VARCHAR(50)   NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_student_contracts_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_contracts_program
        FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE RESTRICT,
    CONSTRAINT chk_student_contracts_amounts_non_negative
        CHECK (total_contract_amount >= 0 AND registration_fee >= 0 AND discount_amount >= 0 AND final_amount >= 0),
    CONSTRAINT chk_student_contracts_discount_not_exceeding_total
        CHECK (discount_amount <= total_contract_amount),
    CONSTRAINT chk_student_contracts_currency_code CHECK (currency_code IN ('USD', 'EUR', 'AFN')),
    CONSTRAINT chk_student_contracts_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_student_contracts_student_id ON student_contracts (student_id);
CREATE INDEX idx_student_contracts_program_id ON student_contracts (program_id);
CREATE INDEX idx_student_contracts_status ON student_contracts (status);

-- ---------------------------------------------------------------------
-- student_documents
-- ---------------------------------------------------------------------
CREATE TABLE student_documents (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id            BIGINT         NOT NULL,
    document_type         VARCHAR(30)    NOT NULL,
    file_path             VARCHAR(500)   NOT NULL,
    original_file_name    VARCHAR(255)   NULL,
    content_type          VARCHAR(100)   NULL,
    file_size_bytes       BIGINT         NULL,
    uploaded_at           DATETIME       NOT NULL,
    expiry_date           DATE           NULL,

    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at            DATETIME       NULL,
    created_at            DATETIME       NOT NULL,
    updated_at            DATETIME       NULL,
    created_by            VARCHAR(50)    NULL,
    updated_by            VARCHAR(50)    NULL,
    version               BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_student_documents_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT chk_student_documents_type
        CHECK (document_type IN ('PASSPORT', 'DIPLOMA', 'TRANSCRIPT', 'ENGLISH_CERTIFICATE', 'CONTRACT', 'OTHER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_student_documents_student_id ON student_documents (student_id);
CREATE INDEX idx_student_documents_type ON student_documents (document_type);

-- =====================================================================
-- SEED DATA
-- =====================================================================

INSERT INTO student_statuses (name, description, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('ACTIVE',    'Currently enrolled and progressing normally', TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('ON_HOLD',   'Temporarily paused (e.g. pending documents)', TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('COMPLETED', 'Program/process completed successfully',      TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('CANCELLED', 'Registration cancelled',                      TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('DEFERRED',  'Enrollment deferred to a later intake',       TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');

INSERT INTO programs (name, destination_country, base_price, currency_code, description,
                       is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('IELTS Preparation & Application',  'United Kingdom', 1200.00, 'USD', 'IELTS coaching plus full UK university application support.', TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Study Visa Package',               'Canada',         1800.00, 'USD', 'Canadian study permit application and college placement.',      TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('University Placement Package',     'Australia',      1600.00, 'USD', 'Australian university placement and visa support.',              TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Undergraduate Placement Package',  'United States',  2200.00, 'USD', 'US undergraduate application and F-1 visa support.',             TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('Language & Pathway Program',       'Germany',         900.00, 'EUR', 'German language course plus pathway program placement.',         TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');
