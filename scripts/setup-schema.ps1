<#
.SYNOPSIS
    Provisions the dedicated 'smartprocure' schema in PostgreSQL smartprocure_db.

.DESCRIPTION
    Safely connects to PostgreSQL as administrator, creates the dedicated schema
    'smartprocure' with ownership assigned to 'smartprocure_app', and configures
    schema-level privileges.
    Enforces least privilege: does NOT grant database-level CREATE to 'smartprocure_app'.
    Leaves all existing tables in the 'public' schema completely untouched.

.PARAMETER SuperuserPassword
    Optional. The 'postgres' superuser password. If omitted, securely prompts for it.

.PARAMETER NonInteractive
    If true, fails immediately if SuperuserPassword is not provided instead of prompting.
#>

[CmdletBinding()]
param(
    [string]$SuperuserPassword = "",
    [switch]$NonInteractive
)

$ErrorActionPreference = 'Stop'

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [SmartProcure] PostgreSQL Dedicated Schema Provisioning" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Locate psql.exe
$psqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psqlPath)) {
    $foundPsql = Get-Command psql.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
    if ($foundPsql) {
        $psqlPath = $foundPsql
    } else {
        throw "PostgreSQL 'psql.exe' could not be found."
    }
}

# Verify service is running
$svc = Get-Service -Name "*postgres*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($svc) {
    if ($svc.Status -ne 'Running') {
        Write-Host "Starting PostgreSQL service ($($svc.Name))..." -ForegroundColor Yellow
        Start-Service -Name $svc.Name
    }
    Write-Host "[OK] PostgreSQL Service is running: $($svc.DisplayName)" -ForegroundColor Green
}

# Determine postgres superuser password securely (masked prompt)
if (-not $SuperuserPassword) {
    if ($env:PGPASSWORD) {
        $SuperuserPassword = $env:PGPASSWORD
    } elseif ($NonInteractive) {
        throw "Superuser password not provided and NonInteractive flag is set."
    } else {
        $secPwd = Read-Host -Prompt "Enter password for PostgreSQL 'postgres' superuser" -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPwd)
        $SuperuserPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

$oldEnv = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $SuperuserPassword

    # Helper to execute superuser queries with strict error abort
    function Invoke-AdminPsql {
        param([string]$Sql)
        $output = & $psqlPath -h localhost -p 5432 -U postgres -d smartprocure_db -v ON_ERROR_STOP=1 -t -A -c $Sql 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "PostgreSQL administrative query failed:`n$output"
        }
        return ($output | Out-String).Trim()
    }

    # 1. Test superuser connection
    Write-Host "Validating PostgreSQL superuser connection..." -ForegroundColor Cyan
    $test = Invoke-AdminPsql "SELECT 1;"
    if ($test -ne "1") {
        throw "Superuser authentication test returned unexpected response: $test"
    }
    Write-Host "[OK] Superuser connected to 'smartprocure_db'." -ForegroundColor Green

    # 2. Configure schema and permissions without granting database-level CREATE
    Write-Host "Creating dedicated schema 'smartprocure' owned by 'smartprocure_app'..." -ForegroundColor Cyan
    $setupSql = @"
GRANT CONNECT, TEMPORARY ON DATABASE smartprocure_db TO smartprocure_app;
CREATE SCHEMA IF NOT EXISTS smartprocure AUTHORIZATION smartprocure_app;
GRANT ALL ON SCHEMA smartprocure TO smartprocure_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA smartprocure GRANT ALL ON TABLES TO smartprocure_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA smartprocure GRANT ALL ON SEQUENCES TO smartprocure_app;
"@
    [void](Invoke-AdminPsql $setupSql)
    Write-Host "[OK] Schema 'smartprocure' created and owned by 'smartprocure_app'." -ForegroundColor Green

    # 3. Post-execution Verification Checks
    Write-Host "`nRunning post-execution verification checks..." -ForegroundColor Cyan

    # Verify schema owner
    $schemaOwner = Invoke-AdminPsql "SELECT schema_owner FROM information_schema.schemata WHERE schema_name = 'smartprocure';"
    if ($schemaOwner -ne "smartprocure_app") {
        throw "Verification failed: Schema 'smartprocure' is owned by '$schemaOwner', expected 'smartprocure_app'."
    }
    Write-Host "[OK] Confirmed schema 'smartprocure' owner: $schemaOwner" -ForegroundColor Green

    # Verify least-privilege: smartprocure_app must NOT have database-level CREATE
    $hasDbCreate = Invoke-AdminPsql "SELECT has_database_privilege('smartprocure_app', 'smartprocure_db', 'CREATE');"
    if ($hasDbCreate -eq "t") {
        throw "Security violation: 'smartprocure_app' unexpectedly has database-level CREATE privilege."
    }
    Write-Host "[OK] Confirmed least-privilege: 'smartprocure_app' does NOT have database-level CREATE (database CREATE = $hasDbCreate)." -ForegroundColor Green

    # Verify public schema tables remain untouched (must be exactly 7)
    $publicCount = Invoke-AdminPsql "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';"
    if ([int]$publicCount -ne 7) {
        throw "Integrity violation: Expected 7 tables in 'public' schema, but found $publicCount."
    }
    Write-Host "[OK] Confirmed public schema integrity: exactly $publicCount existing public tables remain untouched." -ForegroundColor Green

} finally {
    $env:PGPASSWORD = $oldEnv
    $SuperuserPassword = $null
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " [SUCCESS] Dedicated Schema 'smartprocure' Ready!" -ForegroundColor Green
Write-Host " Database: smartprocure_db" -ForegroundColor Green
Write-Host " Schema:   smartprocure (Owner: smartprocure_app)" -ForegroundColor Green
Write-Host " Public:   Preserved (7 legacy tables intact)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
