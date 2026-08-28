-- =====================================================================
-- V1__init.sql
-- HopeStar Finance & Student Management System (HFMS)
-- Phase 1 foundation schema: identity/security tables + audit log.
--
-- Feature-module tables (students, english tests, finance ledger,
-- invoices, reports, backup log, settings) are introduced in their own
-- versioned migrations in later implementation phases, per the approved
-- architecture's Flyway recommendation (§7.3) -- this migration only
-- covers what Phase 1 (project foundation) requires.
--
-- Conventions used throughout (see approved architecture §2):
--   * Surrogate BIGINT AUTO_INCREMENT primary keys.
--   * Every business/master table carries the common audit + soft-delete
--     + optimistic-locking columns from BaseEntity: is_active,
--     deleted_at, created_at, updated_at, created_by, updated_by, version.
--   * All foreign keys use ON DELETE RESTRICT -- financial/security data
--     is never cascade-deleted; soft-delete flags are used instead.
--   * InnoDB engine, utf8mb4 for full Unicode support (names, notes).
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- branches
-- ---------------------------------------------------------------------
CREATE TABLE branches (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_code       VARCHAR(10)  NOT NULL,
    name              VARCHAR(150) NOT NULL,
    address           VARCHAR(255) NULL,
    phone             VARCHAR(20)  NULL,
    email             VARCHAR(100) NULL,
    is_headquarters   BOOLEAN      NOT NULL DEFAULT FALSE,

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NULL,
    created_by        VARCHAR(50)  NULL,
    updated_by        VARCHAR(50)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_branches_code UNIQUE (branch_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- roles
-- ---------------------------------------------------------------------
CREATE TABLE roles (
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

    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- permissions
-- ---------------------------------------------------------------------
CREATE TABLE permissions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(100) NOT NULL,
    description       VARCHAR(255) NULL,
    module            VARCHAR(50)  NULL,

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NULL,
    created_by        VARCHAR(50)  NULL,
    updated_by        VARCHAR(50)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- role_permissions (explicit join entity, not a plain many-to-many)
-- ---------------------------------------------------------------------
CREATE TABLE role_permissions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id           BIGINT       NOT NULL,
    permission_id     BIGINT       NOT NULL,

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NULL,
    created_by        VARCHAR(50)  NULL,
    updated_by        VARCHAR(50)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_role_permissions UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(100) NULL,
    phone                 VARCHAR(20)  NULL,
    role_id               BIGINT       NOT NULL,
    branch_id             BIGINT       NULL,
    last_login_at         DATETIME     NULL,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    account_locked        BOOLEAN      NOT NULL DEFAULT FALSE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,

    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at            DATETIME     NULL,
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME     NULL,
    created_by            VARCHAR(50)  NULL,
    updated_by            VARCHAR(50)  NULL,
    version               BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_users_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_users_branch_id ON users (branch_id);

-- ---------------------------------------------------------------------
-- audit_logs (append-only; deliberately has no BaseEntity columns --
-- see AuditLog entity Javadoc for rationale)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name       VARCHAR(100) NOT NULL,
    entity_id         BIGINT       NOT NULL,
    action            VARCHAR(20)  NOT NULL,
    old_value         LONGTEXT     NULL,
    new_value         LONGTEXT     NULL,
    changed_by        VARCHAR(50)  NULL,
    changed_at        DATETIME     NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_logs_changed_at ON audit_logs (changed_at);

-- =====================================================================
-- SEED DATA
-- =====================================================================

-- Head-office branch. Single-branch deployment today; branch_code 'HQ'
-- is the fixed default referenced by the future branch-aware invoice/
-- student/employee numbering scheme (approved architecture §6) until
-- Multi-branch Support is enabled.
INSERT INTO branches (branch_code, name, address, phone, email, is_headquarters,
                       is_active, created_at, created_by, updated_at, updated_by)
VALUES ('HQ', 'HopeStar Head Office', NULL, NULL, NULL, TRUE,
        TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');

-- Single ADMIN role for go-live. ACCOUNTANT / STAFF roles (per the Future
-- Modules roadmap for multi-user access) are added later as a data
-- change only -- no schema change required.
INSERT INTO roles (name, description, is_active, created_at, created_by, updated_at, updated_by)
VALUES ('ADMIN', 'Full administrative access to all modules', TRUE,
        UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');

-- Baseline permission set covering every module in the approved SRS.
-- Granular enough that future roles (ACCOUNTANT, STAFF) can be assigned
-- meaningful subsets without any schema change.
INSERT INTO permissions (code, description, module, is_active, created_at, created_by, updated_at, updated_by)
VALUES
    ('STUDENT_VIEW',        'View student records',                         'STUDENT',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('STUDENT_MANAGE',      'Create/edit/deactivate student records',       'STUDENT',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('TEST_VIEW',           'View English test registrations',              'ENGLISH_TEST', TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('TEST_MANAGE',         'Manage English test registrations',            'ENGLISH_TEST', TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('FINANCE_VIEW',        'View student payments and ledger',             'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('FINANCE_MANAGE',      'Record student payments and refunds',          'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('SALARY_VIEW',         'View employee salaries',                       'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('SALARY_APPROVE',      'Generate and approve salary runs',             'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('EXPENSE_VIEW',        'View office expenses',                         'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('EXPENSE_APPROVE',     'Approve office expenses',                      'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('LOAN_MANAGE',         'Manage employee loans',                        'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('ADVANCE_MANAGE',      'Manage employee advances',                     'FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('INVOICE_VIEW',        'View invoices',                                'INVOICE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('INVOICE_MANAGE',      'Generate and cancel invoices',                 'INVOICE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('REPORT_VIEW',         'View financial and operational reports',       'REPORT',     TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('TRANSACTION_VOID',    'Void a posted ledger transaction (privileged)','FINANCE',    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('BACKUP_MANAGE',       'Trigger manual backups, view backup history',  'BACKUP',     TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('BACKUP_RESTORE',      'Restore the database from a backup (privileged)', 'BACKUP',  TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('SETTINGS_MANAGE',     'Edit office/system settings',                  'SETTINGS',   TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'),
    ('USER_MANAGE',         'Manage user accounts, roles and permissions',  'AUTH',       TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system');

-- Grant every baseline permission to ADMIN.
INSERT INTO role_permissions (role_id, permission_id, is_active, created_at, created_by, updated_at, updated_by)
SELECT r.id, p.id, TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- Seed ADMIN user.
-- Username: admin
-- Password: Admin@123   (BCrypt strength-12 hash below)
-- must_change_password is TRUE -- the owner is required to set a new
-- password on first login rather than keeping this documented default,
-- per the approved Security Architecture's account-hygiene expectations.
INSERT INTO users (username, password_hash, full_name, email, phone, role_id, branch_id,
                    failed_login_attempts, account_locked, must_change_password,
                    is_active, created_at, created_by, updated_at, updated_by)
SELECT
    'admin',
    '$2b$12$ug2dZlwBAV/weDtxndtW6uvGzUZn7KWTT6xHSjQLqrnQvtv/3JfZO',
    'System Administrator',
    'admin@hopestar.local',
    NULL,
    r.id,
    b.id,
    0, FALSE, TRUE,
    TRUE, UTC_TIMESTAMP(), 'system', UTC_TIMESTAMP(), 'system'
FROM roles r, branches b
WHERE r.name = 'ADMIN' AND b.branch_code = 'HQ';
