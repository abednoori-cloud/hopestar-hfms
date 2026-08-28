# HopeStar Finance & Student Management System (HFMS)

Production system for HopeStar Education Consultancy: student records, English
test registrations, employee finance (salary/loans/advances), office expenses,
invoicing, reporting, and backup/restore — built on the approved Software
Architecture & Implementation Plan.

> **Status:** Phase 1 — Project Foundation.
> Identity/security schema, configuration, and cross-cutting infrastructure
> are in place. Feature modules (Student, English Test, Finance, Invoice,
> Report, Backup, Settings) are implemented incrementally in later phases,
> per the approved architecture's package structure and module boundaries.

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
│   ├── scheduler/              # Scheduled jobs (backup, reminders — later phases)
│   └── module/
│       └── auth/                # User, Role, Permission, RolePermission, Branch
│           (dashboard/, student/, englishtest/, finance/, invoice/, report/,
│            backup/, settings/ modules are added in subsequent phases)
└── src/main/resources/
    ├── application.yml, application-dev.yml, application-prod.yml
    └── db/migration/V1__init.sql
```

This mirrors the package structure agreed in the approved architecture
document exactly — no packages have been renamed, removed, or restructured.

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
| `HFMS_INVOICES_PATH` | generated invoice PDF storage root | `./data/invoices` |
| `HFMS_BACKUP_PATH` | database backup dump storage root | `./data/backups` |

See `application.yml` for the full list of `hfms.*` application-specific
settings (invoice/student/employee code prefixes, backup schedule/retention,
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
database, uploaded documents, generated invoices, backups, and logs.

## 7. Default Administrator Account

Seeded by `V1__init.sql` for first login only:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `Admin@123` |

`must_change_password` is set to `true` on this seed row — the login flow
implemented in Phase 2 (Module 1: Authentication) is required to force a
password change on first login before granting access to the rest of the
system. **Change this password immediately in any non-throwaway
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
  dialect auto-injects tokens into `<form>` elements once the auth module's
  templates are added in Phase 2).
- Accounts lock automatically after 5 failed login attempts
  (`hfms.security.max-failed-login-attempts`).
- Role/permission tables are live in the schema from day one even though
  only the `ADMIN` role exists at go-live — adding `ACCOUNTANT`/`STAFF`
  roles later (per the Future Modules roadmap) is a data change, not a
  schema or code change.

## 10. What's Next (Phase 2+)

Per the approved architecture, subsequent phases add, module by module:
`module/dashboard`, `module/student`, `module/englishtest`,
`module/finance` (ledger, student payments, employee, salary, loan,
advance, expense, refund), `module/invoice`, `module/report`,
`module/backup`, `module/settings`, plus the Thymeleaf templates,
Controllers, and Services for each. No changes to this foundation
(package structure, database design, or configuration) are expected as
those phases land.
