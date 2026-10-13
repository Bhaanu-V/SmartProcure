# SmartProcure

> **AI-Powered Purchase Order Management & Approval System**

SmartProcure is an enterprise procurement management backend built with **Spring Boot 3.3.4** and **PostgreSQL 18**. It automates purchase order lifecycles, risk assessment, and approval routing while enforcing strict least-privilege database security and schema isolation.

---

## Table of Contents
- [1. Project Overview](#1-project-overview)
- [2. Technology Stack](#2-technology-stack)
- [3. Repository Structure](#3-repository-structure)
- [4. Prerequisites](#4-prerequisites)
- [5. Java 21 Setup](#5-java-21-setup)
- [6. Environment Configuration](#6-environment-configuration)
- [7. Database Setup & Schema Isolation](#7-database-setup--schema-isolation)
- [8. Running the Backend](#8-running-the-backend)
- [9. Running Integration & Unit Tests](#9-running-integration--unit-tests)
- [10. Database Migrations with Flyway](#10-database-migrations-with-flyway)
- [Detailed Guides](#detailed-guides)

---

## 1. Project Overview

SmartProcure manages end-to-end procurement workflows, from initial purchase requisition to multi-tier executive approval.

### Current Implementation Status
- **Core Framework**: Spring Boot 3.3.4 application initialized on Eclipse Temurin JDK 21 LTS.
- **Database Architecture**: PostgreSQL 18 integration using a dedicated, isolated schema (`smartprocure`) owned by a least-privilege non-superuser role (`smartprocure_app`).
- **Schema & Migrations**: Automated database migrations managed via Flyway (`V1__init_schema.sql`), provisioning:
  - `users`: User identities, departments, and roles (`REQUESTER`, `APPROVER`, `ADMIN`).
  - `purchase_orders`: Core PO records, currency, total amounts, workflow states, and AI risk scoring fields.
  - `po_line_items`: Granular item descriptions, categories, quantities, unit prices, and line-item totals.
  - `approval_history`: Immutable audit trail tracking workflow decisions and comments.
- **ORM Configuration**: Spring Data JPA / Hibernate 6.5 in schema validation mode (`ddl-auto: validate`) targeting the `smartprocure` schema.
- **Environment Management**: Dynamic `.env` parsing via `dotenv-java` and PowerShell environment scripts.
- **Integration Test Suite**: Automated tests verifying real PostgreSQL connectivity, Flyway migration state, schema isolation, and preservation of legacy tables.

### Planned Functionality (Upcoming Milestones)
- RESTful API controllers and service layers for PO lifecycle CRUD and submission workflows.
- AI risk engine integration for automated purchase order anomaly and risk score analysis.
- Multi-tier approval routing rules based on departmental budget thresholds.
- Single-page application (SPA) frontend dashboard.
- Role-based security (JWT / Spring Security) and user authentication.

---

## 2. Technology Stack

| Layer | Component | Version / Specification |
| :--- | :--- | :--- |
| **Language & Runtime** | Java | Eclipse Temurin JDK 21 LTS |
| **Framework** | Spring Boot | 3.3.4 (Web, Data JPA, Validation, Test) |
| **Database** | PostgreSQL | 18.x (localhost:5432, service `postgresql-x64-18`) |
| **Connection Pool** | HikariCP | 5.1.0 (`SmartProcureHikariCP`) |
| **Migration Tool** | Flyway | 10.x (`flyway-core`, `flyway-database-postgresql`) |
| **ORM / Persistence** | Hibernate / JPA | Hibernate 6.5.3 (`ddl-auto: validate`) |
| **Build Tool** | Apache Maven | Maven Wrapper (`mvnw.cmd` / `mvnw`) |
| **Environment Loader** | dotenv-java | 3.0.0 |
| **DevOps Scripts** | PowerShell | Windows PowerShell 5.1 / PowerShell 7+ |

---

## 3. Repository Structure

```text
C:\SmartProcure\
├── .env.example                               # Local environment template (committed, no secrets)
├── .gitignore                                 # Git exclusions (.env, target/, IDE metadata)
├── README.md                                  # Main project documentation (this file)
│
├── backend/                                   # Spring Boot application root
│   ├── .mvn/wrapper/                          # Maven wrapper configuration & JAR
│   ├── mvnw, mvnw.cmd                         # Maven wrapper executables (Linux / Windows)
│   ├── pom.xml                                # Project dependencies and Maven build plugins
│   └── src/
│       ├── main/
│       │   ├── java/com/smartprocure/
│       │   │   └── SmartProcureApplication.java # Spring Boot entry point & .env loader
│       │   └── resources/
│       │       ├── application.yml            # Primary application configuration
│       │       └── db/migration/
│       │           └── V1__init_schema.sql    # Baseline Flyway migration for smartprocure schema
│       └── test/
│           ├── java/com/smartprocure/
│           │   ├── SmartProcureApplicationTests.java         # Spring context load test
│           │   └── SmartProcureDatabaseIntegrationTests.java # Real PostgreSQL schema integration tests
│           └── resources/
│               └── application-test.yml       # Test profile configuration
│
├── docs/
│   └── DEVELOPMENT_ENVIRONMENT.md             # In-depth setup, IDE settings & troubleshooting
│
└── scripts/
    ├── check-environment.ps1                  # Diagnostic script verifying Java, PostgreSQL & secrets
    ├── load-env.ps1                           # Dot-sources .env variables into active PowerShell session
    ├── setup-db.ps1                           # Automated, least-privilege PostgreSQL & role provisioner
    ├── setup-schema.ps1                       # Dedicated schema ('smartprocure') isolation provisioner
    └── use-java21.ps1                         # Session-scoped Java 21 LTS activator
```

---

## 4. Prerequisites

Before setting up SmartProcure locally, verify that the following prerequisites are installed on your Windows system:

1. **Operating System**: Windows 10 or Windows 11 64-bit with PowerShell.
2. **Java Development Kit**: **Eclipse Temurin JDK 21 LTS**.
   - Standard path: `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
3. **PostgreSQL**: **PostgreSQL 18** installed as a Windows service (`postgresql-x64-18`), listening on port `5432`.
4. **Git**: Git for Windows.

---

## 5. Java 21 Setup

SmartProcure requires **Java 21 LTS**. On Windows machines where newer Java versions (such as Oracle JDK 25) co-exist, Windows system shortcuts can prioritize Java 25 over JDK 21.

To resolve this safely **without uninstalling Java 25 or modifying global Windows environment variables**, SmartProcure provides a session-scoped activation script:

### Activating Java 21 in PowerShell
Open PowerShell in the project directory and run:
```powershell
. .\scripts\use-java21.ps1
```
> **Note**: The leading dot and space (`. `) dot-sources the script, applying `$env:JAVA_HOME` and the updated `$env:PATH` to your active terminal session.

### Verifying Java Version
```powershell
java -version
javac -version
```
Expected output:
```text
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS)
javac 21.0.12.1
```

---

## 6. Environment Configuration

SmartProcure separates application code from sensitive environment configurations using a root `.env` file.

### Step 1: Create Your Local `.env` File
Copy the provided template to `.env`:
```powershell
Copy-Item .env.example .env
```

### Step 2: Understand the Configuration Variables

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `DB_URL` | JDBC URL pointing to PostgreSQL with dedicated schema parameter | `jdbc:postgresql://localhost:5432/smartprocure_db?currentSchema=smartprocure` |
| `DB_USERNAME` | Non-superuser application role | `smartprocure_app` |
| `DB_PASSWORD` | Secure local application role password | *(Generated during database setup)* |
| `PGHOST` | PostgreSQL host for client utilities | `localhost` |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `smartprocure_db` |
| `PGUSER` | Database role for client utilities | `smartprocure_app` |
| `PGOPTIONS` | Search path parameter for client tools | `--search_path=smartprocure` |
| `SERVER_PORT` | Spring Boot HTTP listening port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `JAVA_HOME` | Path to your JDK 21 installation | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |

> **CRITICAL SECURITY RULE**:
> - Never commit `.env` to Git. `.gitignore` is pre-configured to strictly exclude `.env`.
> - Never put real production passwords or API tokens into `.env.example` or documentation.

---

## 7. Database Setup & Schema Isolation

SmartProcure enforces **Option A Schema Isolation**: all application tables reside in a dedicated schema named `smartprocure`.

### Security & Integrity Principles
- **Least Privilege**: The application connects exclusively as `smartprocure_app`, which is granted ownership of schema `smartprocure` but has **no superuser rights** and **no database-level `CREATE` privilege**.
- **Legacy Preservation**: Any existing tables in the `public` schema (e.g. 7 pre-existing legacy tables) remain completely untouched and preserved.
- **Never Drop the Database**: Never run `DROP DATABASE` or truncate existing database objects.

### Provisioning Scripts
Two PowerShell scripts manage database configuration:

#### Option 1: Automated Full Provisioning (`scripts\setup-db.ps1`)
Use this script for initial setup or setting up a new development environment:
```powershell
.\scripts\setup-db.ps1
```
- **What it does**:
  1. Securely prompts for the `postgres` administrator password (input is masked; not stored or printed).
  2. Creates or verifies the database `smartprocure_db`.
  3. Creates the non-superuser application role `smartprocure_app`.
  4. Generates an unbiased 32-character cryptographic password (or preserves an existing `.env` password).
  5. Creates schema `smartprocure` with authorization `smartprocure_app`.
  6. Writes/updates the local `.env` configuration file safely.
  7. Tests connectivity as `smartprocure_app`.

#### Option 2: Dedicated Schema Isolation Setup (`scripts\setup-schema.ps1`)
Use this script if the database and role already exist and you only need to ensure the `smartprocure` schema permissions and verify public schema integrity:
```powershell
.\scripts\setup-schema.ps1
```

---

## 8. Running the Backend

Follow these verified steps to run the Spring Boot backend service:

```powershell
# 1. Activate Java 21 for the current session
. .\scripts\use-java21.ps1

# 2. Load .env environment variables into the session
. .\scripts\load-env.ps1

# 3. Change directory to backend
Set-Location C:\SmartProcure\backend

# 4. Start the application with Maven Wrapper
.\mvnw.cmd spring-boot:run
```

Once started, the backend API is accessible at:
```text
http://localhost:8080
```
Spring Boot automatically applies any pending Flyway migrations in `smartprocure` and validates entity schemas via Hibernate upon startup.

---

## 9. Running Integration & Unit Tests

### Executing the Test Suite
To run the automated tests:
```powershell
# In PowerShell:
. .\scripts\use-java21.ps1
. .\scripts\load-env.ps1
Set-Location C:\SmartProcure\backend
.\mvnw.cmd clean test
```

### Understanding the Tests
- **Database Dependency**: The test suite **requires a live, running local PostgreSQL instance** (`smartprocure_db`) with credentials configured in `.env`. The tests are **not mock-based** and do not use in-memory H2 databases.
- **[`SmartProcureApplicationTests`](backend/src/test/java/com/smartprocure/SmartProcureApplicationTests.java)**: Validates that the Spring application context boots cleanly and validates JPA entities against the database.
- **[`SmartProcureDatabaseIntegrationTests`](backend/src/test/java/com/smartprocure/SmartProcureDatabaseIntegrationTests.java)**:
  - Connects to PostgreSQL using `JdbcTemplate` via `HikariCP`.
  - Asserts that session `current_schema()` resolves to `smartprocure`.
  - Verifies Flyway migration `V1` is applied successfully in `smartprocure.flyway_schema_history`.
  - Asserts that domain tables (`users`, `purchase_orders`, `po_line_items`, `approval_history`) exist in `smartprocure`.
  - Asserts that all 7 legacy tables in schema `public` remain intact and untouched.

---

## 10. Database Migrations with Flyway

SmartProcure relies exclusively on **Flyway** to manage database schema evolution. Hibernate DDL generation (`ddl-auto: update` or `create`) is strictly disabled to prevent untracked schema alterations.

### Where Migrations Live
Flyway migration SQL files are located in:
```text
backend/src/main/resources/db/migration/
```

### Migration Naming Convention
All migrations follow Flyway's standard versioning format:
```text
V<VERSION>__<Description_With_Underscores>.sql
```
- Starts with uppercase `V`.
- Followed by a sequential version number (e.g. `1`, `2`, `3`).
- Separated by **two underscores** (`__`).
- Ends with a short description in snake_case and `.sql`.

Examples:
- `V1__init_schema.sql` *(Current baseline)*
- `V2__add_vendor_catalog_table.sql` *(Example future migration)*

### Rules for Schema Changes
1. **Never edit an already-applied migration**: Once a migration has been applied to any environment, its checksum is stored in `smartprocure.flyway_schema_history`. Modifying the file will cause checksum validation errors on startup.
2. **Always create a new version**: If you need to add columns, alter constraints, or create tables, create a new file (e.g. `V2__...sql`).
3. **Always target the smartprocure schema**: Migrations execute in the `smartprocure` default schema. Do not write SQL referencing or altering `public`.

---

## Detailed Guides

For deeper technical guides, IDE configuration instructions (IntelliJ IDEA, VS Code), and troubleshooting steps, refer to:
- [docs/DEVELOPMENT_ENVIRONMENT.md](docs/DEVELOPMENT_ENVIRONMENT.md)
