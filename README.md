# HopeStar Finance & Student Management System (HFMS)

Production system for HopeStar Education Consultancy: student records, English
test registrations, employee finance (salary/loans/advances), office expenses,
PDF receipts/vouchers, reporting, and backup/restore — built on the approved
Software Architecture & Implementation Plan.

> **Status:** Feature-complete and deployed.
> All modules in the approved architecture are implemented, tested, and
> working — Authentication/Security, Student Management, Finance Foundation
> (currencies, payment methods, ledger), Student Payments, English Test
> Management, Employee Management, Salary Management, Employee Loans,
> Employee Advances, Expenses, PDF Receipts & Vouchers, Reports (with PDF
> export), Backup & Restore, and Settings/System Configuration. Packaged as
> a Windows installer with an automated launcher, and has been deployed to
> a real end user's machine. See [§11](#11-known-limitations--outstanding-work)
> for what's still outstanding.

---

## 1. Technology Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security (session-based form login, BCrypt-12, RBAC) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8 |
| Schema migrations | Flyway |
| View layer | Thymeleaf + Bootstrap 5 |
| Build | Maven |
| Boilerplate reduction | Lombok |
| Validation | Jakarta Bean Validation |

## 2. Prerequisites

- JDK 21
- Maven 3.9+ (or use the included Docker build, which needs no local Maven)
- MySQL 8.x running locally, **or** Docker + Docker Compose

## 3. Project Structure

```
hopestar-hfms/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── src/main/java/com/hopestar/hfms/
│   ├── HfmsApplication.java
│   ├── config/              # Security, persistence, scheduler, file storage,
│   │                         # audit, web MVC configuration
│   ├── common/               # BaseEntity, shared enums, DTOs, exceptions, utils
│   ├── security/              # UserPrincipal, CustomUserDetailsService
│   ├── audit/                 # AuditLog entity + JPA entity listener
│   ├── scheduler/              # Scheduled jobs (automatic backups, etc.)
│   ├── launcher/               # Windows installer launcher: MySQL setup
│   │                            # detection, elevation, health-check polling,
│   │                            # browser auto-open, system tray icon
│   └── module/
│       ├── auth/                # User, Role, Permission, RolePermission,
│       │                         # Branch, organization settings
│       ├── dashboard/            # Dashboard summary views
│       ├── student/              # Student records + English test registrations
│       ├── finance/              # ledger, studentpayment, employee, salary,
│       │                         # loan, advance, expense
│       ├── reports/              # Cashflow, student, employee reports (PDF export)
│       └── backup/               # Manual + schedulable automatic backup/restore
└── src/main/resources/
    ├── application.yml, application-dev.yml, application-prod.yml
    └── db/migration/
```

This mirrors the package structure agreed in the approved architecture
document — no packages have been renamed, removed, or restructured from
what was originally planned.

## 4. Configuration

Configuration is environment-variable driven so the same jar/image runs in
dev, staging, and production without code changes.

| Variable | Purpose | Default (dev) |
|---|---|---|
| `HFMS_PROFILE` | active Spring profile (`dev` / `prod`) | `dev` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | database connection | `localhost`, `3306`, `hfms_dev` |
| `DB_USERNAME`, `DB_PASSWORD` | database credentials | `hfms_user` / `hfms_password` |
| `SERVER_PORT` | HTTP port | `8080` |
| `HFMS_DOCS_PATH` | student document storage root | `./data/documents` |
| `HFMS_RECEIPTS_PATH` | reserved storage root for a possible future "persist generated receipt/voucher PDFs" feature (unused today -- PDFs are generated on demand, never written to disk) | `./data/receipts` |
| `HFMS_BACKUP_PATH` | database backup dump storage root | `./data/backups` |

See `application.yml` for the full list of `hfms.*` application-specific
settings (student/employee code prefixes, backup schedule,
account-lockout threshold).

## 5. Running Locally (without Docker)

```bash
# 1. Create the dev database (Flyway will create tables automatically
#    on first startup via V1__init.sql)
mysql -u root -p -e "CREATE DATABASE hfms_dev CHARACTER SET utf8mb4;"
mysql -u root -p -e "CREATE USER 'hfms_user'@'%' IDENTIFIED BY 'hfms_password';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON hfms_dev.* TO 'hfms_user'@'%';"

# 2. Build
mvn clean package

# 3. Run
java -jar target/hfms.jar
# or, for hot reload during development:
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Flyway applies `V1__init.sql`
automatically on first boot, creating the identity/security schema and
seeding the default administrator account (see §7 below).

## 6. Running with Docker Compose

```bash
cp .env.example .env
# edit .env with real credentials, then:
docker compose up --build
```

This starts MySQL 8 and the HFMS app together, with named volumes for the
database, uploaded documents, receipts, backups, and logs.

## 7. Default Administrator Account

Seeded by `V1__init.sql` for first login only:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `Admin@123` |

`must_change_password` is set to `true` on this seed row — the login flow
forces a password change on first login before granting access to the rest
of the system. **Change this password immediately in any non-throwaway
environment.**

## 8. Database Migrations

All schema changes are managed by Flyway — **never** by
`hibernate.ddl-auto` (which is set to `validate` on purpose, so the app
fails fast at startup if the entities and the migrated schema disagree).

New migrations go in `src/main/resources/db/migration/`, named
`V{n}__description.sql`, and are applied automatically on startup.

## 9. Security Notes

- Passwords hashed with BCrypt, strength 12.
- CSRF protection is enabled by default (Thymeleaf's Spring Security
  dialect auto-injects tokens into all `<form>` elements).
- Accounts lock automatically after 5 failed login attempts
  (`hfms.security.max-failed-login-attempts`).
- Role/permission tables are live in the schema from day one even though
  only the `ADMIN` role exists at go-live — adding `ACCOUNTANT`/`STAFF`
  roles later is a data change, not a schema or code change.
- HTTPS is supported for production/installer builds via a self-signed
  certificate generated at build time with `keytool` (the dev workflow
  itself is unaffected and still runs over plain HTTP).

## 10. Windows Installer

A jpackage-based Windows installer bundles the app with an automated
launcher that, on first run, detects whether MySQL is installed, requests
elevation when needed, runs headless MySQL installation/initialization
(root password is never persisted to disk), polls the app's health-check
endpoint until it's up, and then opens the default browser to the app —
with a system tray icon for subsequent launches. This has been deployed
once, successfully, to a real end user's fresh Windows machine.

## 11. Known Limitations / Outstanding Work

- **Backup path configurability** — the backup feature's `mysqldump`/`mysql`
  invocation currently assumes the path used during development. On an
  installation where MySQL is installed to a different path, backups can
  fail until this is made configurable or auto-detected per-installation.
- **No bulk "reset to empty" tool** — there's currently no safe way for a
  customer to clear out test data in bulk when transitioning from testing
  to real production use.
- **Hardening is ongoing, not a discrete phase** — this project was not
  built in the phase sequence originally planned; instead, hardening and
  polish have happened continuously alongside feature work, and continue
  to happen as issues surface in real use.
