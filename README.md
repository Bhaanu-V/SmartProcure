# SmartProcure

> **AI-Powered Purchase Order Management & Approval System**

SmartProcure automates enterprise procurement workflows, purchase order generation, risk analysis, and multi-tier approval routing using intelligent automation and Spring Boot.

---

## Quick Start Guide

### 1. Prerequisites
- **Windows 11 64-bit**
- **Eclipse Temurin JDK 21 LTS** (Installed at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`)
- **PostgreSQL 18** (Windows Service `postgresql-x64-18` running on port 5432)
- **Git** & **Node.js**

### 2. Configure Your Terminal Session (Java 21)
To ensure Java 21 takes precedence over any other installed JDKs (such as Java 25):
```powershell
. .\scripts\use-java21.ps1
```

### 3. Initialize the Database
Set up the `smartprocure_db` database and `smartprocure_app` role:
```powershell
.\scripts\setup-db.ps1
```
*(This script will prompt for the superuser `postgres` password, generate a secure application role password, and output a local `.env` file).*

### 4. Verify Your Environment
Run the diagnostic test suite to ensure all services and tools are healthy:
```powershell
.\scripts\check-environment.ps1
```

### 5. Build and Run the Backend
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The application will be accessible at `http://localhost:8080`.

---

## Detailed Documentation
For deep-dive instructions, IDE configuration, and troubleshooting, consult [docs/DEVELOPMENT_ENVIRONMENT.md](docs/DEVELOPMENT_ENVIRONMENT.md).
