<#
.SYNOPSIS
    Provisions the SmartProcure development database and application role in PostgreSQL.

.DESCRIPTION
    Safely inspects PostgreSQL, creates the dedicated application role 'smartprocure_app'
    with a strong generated password (or specified password), creates 'smartprocure_db',
    grants appropriate least-privilege permissions, updates '.env', and verifies connectivity.
    Never exposes passwords in stdout logs or commits them to Git.

.PARAMETER SuperuserPassword
    Optional. The 'postgres' superuser password. If omitted, the script securely prompts for it.

.PARAMETER AppPassword
    Optional. Desired password for 'smartprocure_app'. If omitted, a strong 24-character random password is generated.

.PARAMETER NonInteractive
    If true, fails immediately if SuperuserPassword is not provided instead of prompting.
#>

[CmdletBinding()]
param(
    [string]$SuperuserPassword = "",
    [string]$AppPassword = "",
    [switch]$NonInteractive
)

$ErrorActionPreference = 'Stop'

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [SmartProcure] PostgreSQL Database & Role Setup" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Locate psql.exe
$psqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psqlPath)) {
    $foundPsql = Get-Command psql -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
    if ($foundPsql) {
        $psqlPath = $foundPsql
    } else {
        Write-Error "PostgreSQL 'psql.exe' could not be found. Please check PostgreSQL installation."
        return
    }
}
Write-Host "[OK] Using psql: $psqlPath" -ForegroundColor Green

# Verify service is running
$svc = Get-Service -Name "*postgres*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($svc) {
    if ($svc.Status -ne 'Running') {
        Write-Host "Starting PostgreSQL service ($($svc.Name))..." -ForegroundColor Yellow
        Start-Service -Name $svc.Name
    }
    Write-Host "[OK] PostgreSQL Service is running: $($svc.DisplayName)" -ForegroundColor Green
}

# Determine postgres superuser password
if (-not $SuperuserPassword) {
    if ($env:PGPASSWORD) {
        $SuperuserPassword = $env:PGPASSWORD
    } elseif ($NonInteractive) {
        Write-Error "Superuser password not provided and NonInteractive flag is set."
        return
    } else {
        $secPwd = Read-Host -Prompt "Enter password for PostgreSQL 'postgres' superuser" -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPwd)
        $SuperuserPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

# Helper to execute SQL as postgres superuser
function Invoke-PostgresSuperQuery {
    param([string]$Query, [string]$Database = "postgres")
    $oldEnv = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $SuperuserPassword
        $res = & $psqlPath -h localhost -p 5432 -U postgres -d $Database -t -A -c $Query 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "psql failed: $res"
        }
        return ($res | Out-String).Trim()
    } finally {
        $env:PGPASSWORD = $oldEnv
    }
}

# Test superuser authentication
Write-Host "Validating PostgreSQL superuser connection..." -ForegroundColor Cyan
try {
    $dbTest = Invoke-PostgresSuperQuery "SELECT 1;"
    if ($dbTest -ne "1") {
        Write-Error "Unexpected response from PostgreSQL: $dbTest"
        return
    }
    Write-Host "[OK] Connected to PostgreSQL superuser successfully." -ForegroundColor Green
} catch {
    Write-Host "`n[ERROR] Superuser authentication failed. Please check the 'postgres' password." -ForegroundColor Red
    Write-Host "$_" -ForegroundColor Red
    return
}

# Check or generate application role password
if (-not $AppPassword) {
    # Generate 24-char cryptographic random alphanumeric password
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#%*+,-./:;=?@^_"
    $appPwdBuilder = New-Object System.Text.StringBuilder
    foreach ($b in $bytes) {
        [void]$appPwdBuilder.Append($chars[$b % $chars.Length])
    }
    $AppPassword = $appPwdBuilder.ToString()
}

# Check if role 'smartprocure_app' exists
Write-Host "`nChecking application role 'smartprocure_app'..." -ForegroundColor Cyan
$roleExists = Invoke-PostgresSuperQuery "SELECT 1 FROM pg_roles WHERE rolname='smartprocure_app';"
if ($roleExists -eq "1") {
    Write-Host "[OK] Role 'smartprocure_app' already exists. Updating password..." -ForegroundColor Yellow
    # Escape single quotes in password if any
    $escapedPwd = $AppPassword.Replace("'", "''")
    [void](Invoke-PostgresSuperQuery "ALTER ROLE smartprocure_app WITH PASSWORD '$escapedPwd';")
    Write-Host "[OK] Role 'smartprocure_app' password updated." -ForegroundColor Green
} else {
    Write-Host "Creating dedicated role 'smartprocure_app'..." -ForegroundColor Cyan
    $escapedPwd = $AppPassword.Replace("'", "''")
    [void](Invoke-PostgresSuperQuery "CREATE ROLE smartprocure_app WITH LOGIN PASSWORD '$escapedPwd' NOSUPERUSER NOCREATEDB NOCREATEROLE;")
    Write-Host "[OK] Role 'smartprocure_app' created successfully." -ForegroundColor Green
}

# Check if database 'smartprocure_db' exists
Write-Host "`nChecking database 'smartprocure_db'..." -ForegroundColor Cyan
$dbExists = Invoke-PostgresSuperQuery "SELECT 1 FROM pg_database WHERE datname='smartprocure_db';"
if ($dbExists -eq "1") {
    Write-Host "[OK] Database 'smartprocure_db' already exists." -ForegroundColor Green
} else {
    Write-Host "Creating database 'smartprocure_db' with owner 'smartprocure_app'..." -ForegroundColor Cyan
    [void](Invoke-PostgresSuperQuery "CREATE DATABASE smartprocure_db WITH OWNER smartprocure_app ENCODING 'UTF8';")
    Write-Host "[OK] Database 'smartprocure_db' created successfully." -ForegroundColor Green
}

# Configure database permissions
Write-Host "Configuring privileges for 'smartprocure_app' on 'smartprocure_db'..." -ForegroundColor Cyan
$grantDbSql = @"
GRANT CONNECT, TEMPORARY ON DATABASE smartprocure_db TO smartprocure_app;
"@
[void](Invoke-PostgresSuperQuery $grantDbSql)

$grantSchemaSql = @"
GRANT ALL ON SCHEMA public TO smartprocure_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO smartprocure_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO smartprocure_app;
"@
[void](Invoke-PostgresSuperQuery $grantSchemaSql -Database "smartprocure_db")
Write-Host "[OK] Database privileges configured successfully." -ForegroundColor Green

# Update / Create .env file safely
$workspaceRoot = "C:\SmartProcure"
$envPath = Join-Path $workspaceRoot ".env"

$envContent = @"
# SmartProcure - Local Environment Configuration (Generated)
# DO NOT COMMIT THIS FILE TO VERSION CONTROL

# Database Connectivity
DB_URL=jdbc:postgresql://localhost:5432/smartprocure_db
DB_USERNAME=smartprocure_app
DB_PASSWORD=$AppPassword

# PostgreSQL Client Tools
PGHOST=localhost
PGPORT=5432
PGDATABASE=smartprocure_db
PGUSER=smartprocure_app

# Application Profiles
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Java Home
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
"@

Set-Content -Path $envPath -Value $envContent -Encoding utf8
Write-Host "[OK] Saved local credentials to '$envPath'." -ForegroundColor Green

# Verify connection as application role
Write-Host "`nVerifying connection as application role 'smartprocure_app'..." -ForegroundColor Cyan
$oldEnv = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $AppPassword
    $appTest = & $psqlPath -h localhost -p 5432 -U smartprocure_app -d smartprocure_db -t -A -c "SELECT current_user, current_database(), session_user;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Application connection verified: $appTest" -ForegroundColor Green
    } else {
        Write-Error "Application connection test failed: $appTest"
        return
    }

    # Verify not superuser
    $isSuper = & $psqlPath -h localhost -p 5432 -U smartprocure_app -d smartprocure_db -t -A -c "SELECT usesuper FROM pg_user WHERE usename = 'smartprocure_app';" 2>&1
    if (($isSuper | Out-String).Trim() -eq "f") {
        Write-Host "[OK] Confirmed 'smartprocure_app' is a non-superuser role (least privilege enforced)." -ForegroundColor Green
    }
} finally {
    $env:PGPASSWORD = $oldEnv
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " [SUCCESS] SmartProcure Database Setup Completed!" -ForegroundColor Green
Write-Host " Database: smartprocure_db" -ForegroundColor Green
Write-Host " Role:     smartprocure_app" -ForegroundColor Green
Write-Host " Config:   $envPath" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
