# SmartProcure - Development Environment Setup & Troubleshooting Guide

This guide documents the configuration of the local Windows 11 development environment for **SmartProcure (AI-Powered Purchase Order Management & Approval System)**.

---

## 1. System Overview & Detected Environment

| Component | Status | Detected Path / Version | Notes |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 11 64-bit | Build 26200 (Home Single Language) | System Drive: `C:\` |
| **Project Workspace** | Configured | `C:\SmartProcure` | Clean Git repo initialized |
| **Active Target JDK** | **Eclipse Temurin JDK 21 LTS** | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` | Verified `java.exe` & `javac.exe` |
| **Co-existing JDK** | Oracle JDK 25 LTS | `C:\Program Files\Java\jdk-25` | Preserved; undamaged |
| **Database** | **PostgreSQL 18** | `C:\Program Files\PostgreSQL\18` | Port `5432` |
| **Database Service** | `postgresql-x64-18` | Status: `Running` (Automatic start) | Windows Service |
| **Build Tooling** | Maven Wrapper 3.9.16 | `C:\SmartProcure\backend\mvnw.cmd` | Java 21 compatible |
| **Node.js & npm** | Node v24.18.0 / npm 11.16.0 | `C:\Program Files\nodejs` | Installed & verified |
| **Git** | Git 2.54.0 | `C:\Program Files\Git\cmd\git.exe` | Staging clean |

---

## 2. Java 21 LTS Configuration & Precedence Fix

### Why Java 25 was taking precedence:
Windows sets `C:\Program Files\Common Files\Oracle\Java\javapath` near the beginning of the system `PATH`. When typing `java` or `javac` in a default terminal, Windows resolved to Oracle's stub, invoking Java 25 even though Eclipse Temurin JDK 21 was installed and Machine `JAVA_HOME` was configured.

### The Project-Specific Solution:
To avoid modifying global Windows system settings or breaking other projects that rely on Java 25, SmartProcure uses session-scoped environment activation:

1. **Activation Script (`scripts\use-java21.ps1`)**:
   - Automatically discovers the verified Eclipse Temurin JDK 21 installation at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
   - Sets `$env:JAVA_HOME` for the current PowerShell session.
   - Prepends the JDK 21 `bin` directory to the session `$env:PATH`, ensuring JDK 21 takes precedence over Oracle's `javapath`.
   - Also appends PostgreSQL client tools (`C:\Program Files\PostgreSQL\18\bin`) to `$env:PATH`.
   - Verifies `java -version` and `javac -version`.

### How to Activate Java 21 in PowerShell:
In any PowerShell terminal before working on SmartProcure, run:
```powershell
. .\scripts\use-java21.ps1
```
*(Note the leading dot and space `. `, which dot-sources the variables into your active terminal session).*

### How to Verify:
```powershell
java -version
javac -version
```
Expected output:
```text
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS, mixed mode, sharing)
javac 21.0.12.1
```

### Configuring IDEs (VS Code / IntelliJ IDEA / Antigravity IDE):
- **IntelliJ IDEA**: Go to `File` > `Project Structure` > `Project SDK` > Select `Add SDK` > `JDK...` > Browse to `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
- **VS Code**: In `.vscode/settings.json`, set:
  ```json
  {
    "java.jdt.ls.java.home": "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot",
    "java.configuration.runtimes": [
      {
        "name": "JavaSE-21",
        "path": "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot",
        "default": true
      }
    ]
  }
  ```

---

## 3. PostgreSQL Database & Role Configuration

SmartProcure is strictly architected for **PostgreSQL 18** (MySQL is prohibited).

### Architecture & Security Requirements:
- **Server**: PostgreSQL 18 Windows Service (`postgresql-x64-18`) on `localhost:5432`.
- **Database Name**: `smartprocure_db`
- **Application Role**: `smartprocure_app` (dedicated least-privilege role, `NOSUPERUSER`, `NOCREATEDB`, `NOCREATEROLE`).
- **Target Schema**: `smartprocure` (dedicated isolated schema; legacy `public` schema tables remain untouched).
- **Flyway Migrations**: Applied automatically on startup via `db/migration/V1__init_schema.sql` into schema `smartprocure`.
- **Hibernate DDL**: `validate` mode in production to protect schema integrity (`default_schema: smartprocure`).

### Provisioning the Database:
To create the database and role securely without exposing passwords in terminal history:
```powershell
.\scripts\setup-db.ps1
```
The script will:
1. Prompt securely for your PostgreSQL `postgres` superuser password (using masked input).
2. Generate a cryptographically secure 24-character random password for `smartprocure_app`.
3. Create `smartprocure_db` with owner `smartprocure_app`.
4. Grant standard schema and sequence privileges.
5. Create or update your private local `.env` file with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
6. Verify that `smartprocure_app` connects successfully.

---

## 4. Managing Environment Variables Safely

### Local Secrets Policy:
- Real database credentials belong **only** in `.env` (or OS environment variables).
- `.env` and all credential patterns are strictly excluded in `.gitignore`.
- `.env.example` contains placeholders and is committed to Git.

### Loading Environment Variables in PowerShell:
To load `.env` into your active PowerShell session:
```powershell
. .\scripts\load-env.ps1
```

### How Spring Boot Loads Configuration:
The backend uses a multi-layered approach:
1. **Dynamic Dotenv Loader**: In `SmartProcureApplication.java`, `io.github.cdimascio:dotenv-java` detects `.env` in the root workspace and maps variables to System properties if not already set.
2. **Spring Datasource Resolution**: `application.yml` maps:
   - `spring.datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/smartprocure_db?currentSchema=smartprocure}`
   - `spring.datasource.username: ${DB_USERNAME:smartprocure_app}`
   - `spring.datasource.password: ${DB_PASSWORD}`

---

## 5. Building and Running the Backend

### Test & Compile with Maven Wrapper:
```powershell
. .\scripts\use-java21.ps1
cd backend
.\mvnw.cmd clean test-compile
```

### Run Full Build / Tests:
```powershell
.\mvnw.cmd test
```

### Run the Spring Boot Server:
```powershell
.\mvnw.cmd spring-boot:run
```
The backend API starts on port `8080`.

---

## 6. Diagnostic & Environment Verification

To verify that all environment components are in working order at any time:
```powershell
.\scripts\check-environment.ps1
```

---

## 7. Troubleshooting Guide

### Issue 1: `java -version` returns Java 25
- **Cause**: Oracle's javapath is placed earlier in the Windows System PATH than Adoptium.
- **Remedy**: Always run `. .\scripts\use-java21.ps1` in your PowerShell terminal before building or running commands.

### Issue 2: `psql` is not recognized
- **Cause**: PostgreSQL binary directory `C:\Program Files\PostgreSQL\18\bin` is not in the default Windows PATH.
- **Remedy**: Run `. .\scripts\use-java21.ps1` (which automatically adds it for the session) or add `C:\Program Files\PostgreSQL\18\bin` to your User PATH in Windows System Settings.

### Issue 3: PostgreSQL service not running
- **Cause**: Service stopped or set to manual.
- **Remedy**: Check service status with `Get-Service postgresql-x64-18`. If stopped, start it via PowerShell as Administrator:
  ```powershell
  Start-Service postgresql-x64-18
  ```

### Which Operations Require Administrator Privileges:
- Starting or stopping Windows services (e.g. `postgresql-x64-18`) if permissions are restricted.
- Modifying machine-wide environment variables (e.g. System PATH or Machine JAVA_HOME).
- All SmartProcure project scripts (`use-java21.ps1`, `setup-db.ps1`, `load-env.ps1`, `check-environment.ps1`) run entirely in **User space without requiring Administrator elevation**.
