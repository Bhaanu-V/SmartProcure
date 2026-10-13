<#
.SYNOPSIS
    SmartProcure Environment Verification Script

.DESCRIPTION
    Runs safe, non-destructive diagnostic checks verifying:
    1. Java Runtime & Compiler (Java 21 LTS)
    2. JAVA_HOME and Path resolution
    3. Maven Wrapper availability
    4. PostgreSQL 18 Service status & port 5432
    5. Database (smartprocure_db) and Role (smartprocure_app) connectivity
    6. Git protection & Secret exclusion (.gitignore)

.EXAMPLE
    .\scripts\check-environment.ps1
#>

[CmdletBinding()]
param()

$report = [System.Collections.Generic.List[PSCustomObject]]::new()

function Add-CheckResult {
    param(
        [string]$Category,
        [string]$Check,
        [bool]$Passed,
        [string]$Details
    )
    $report.Add([PSCustomObject]@{
        Category = $Category
        Check    = $Check
        Status   = if ($Passed) { "PASS" } else { "FAIL" }
        Details  = $Details
    })
    $color = if ($Passed) { "Green" } else { "Red" }
    $mark = if ($Passed) { "[PASS]" } else { "[FAIL]" }
    Write-Host "$mark $Category - $Check : $Details" -ForegroundColor $color
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [SmartProcure] Verifying Development Environment" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Java Runtime
$javaVerRaw = (cmd.exe /c "java -version 2>&1") -join "`n"
$isJava21 = [bool]($javaVerRaw -match 'version "(21[^\"]*)"')
$matchedVer = if ($isJava21) { $Matches[1] } else { "Non-21" }
$activeJavaPath = (Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
Add-CheckResult "Java" "Runtime Version is 21" $isJava21 "Active: $activeJavaPath (v$matchedVer)"

# 2. Java Compiler
$javacVerRaw = cmd.exe /c "javac -version 2>&1"
$isJavac21 = $javacVerRaw -match 'javac 21'
Add-CheckResult "Java" "Compiler Version is 21" $isJavac21 "$javacVerRaw"

# 3. JAVA_HOME
$javaHome = $env:JAVA_HOME
$validJavaHome = ($javaHome -and (Test-Path "$javaHome\bin\java.exe") -and (Test-Path "$javaHome\bin\javac.exe") -and ($javaHome -match '21'))
Add-CheckResult "Java" "JAVA_HOME points to JDK 21" $validJavaHome "JAVA_HOME=$javaHome"

# 4. Maven Wrapper
$mvnwPath = "C:\SmartProcure\backend\mvnw.cmd"
$hasMvnw = Test-Path $mvnwPath
Add-CheckResult "Build" "Maven Wrapper Present" $hasMvnw "Path: $mvnwPath"

# 5. PostgreSQL Service
$pgSvc = Get-Service -Name "*postgres*" -ErrorAction SilentlyContinue | Select-Object -First 1
$svcRunning = ($pgSvc -and $pgSvc.Status -eq 'Running')
Add-CheckResult "Database" "PostgreSQL Service Running" $svcRunning "Service: $($pgSvc.DisplayName) [$($pgSvc.Status)]"

# 6. PostgreSQL Port 5432
$portConn = Get-NetTCPConnection -LocalPort 5432 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
$portListening = ($portConn -ne $null)
Add-CheckResult "Database" "Port 5432 Listening" $portListening "Port 5432 listening by PID $($portConn.OwningProcess)"

# 7. Database Credentials in .env
$envFile = "C:\SmartProcure\.env"
$envExists = Test-Path $envFile
Add-CheckResult "Config" "Local .env File Exists" $envExists "Path: $envFile"

# 8. Test Database Connection using smartprocure_app (if .env exists)
$dbConnected = $false
$dbDetail = "No .env credentials found"
if ($envExists) {
    # Parse .env
    $dbPass = ""
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*DB_PASSWORD\s*=\s*(.*)$') {
            $dbPass = $matches[1].Trim().Trim('"').Trim("'")
        }
    }

    $psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
    if ((Test-Path $psql) -and $dbPass -and ($dbPass -ne "your_secure_local_password_here")) {
        $oldPgp = $env:PGPASSWORD
        try {
            $env:PGPASSWORD = $dbPass
            $testOut = cmd.exe /c "`"$psql`" -h localhost -p 5432 -U smartprocure_app -d smartprocure_db -t -A -c `"SELECT current_database(), current_user;`" 2>&1"
            if ($LASTEXITCODE -eq 0 -and $testOut -match 'smartprocure_db\|smartprocure_app') {
                $dbConnected = $true
                $dbDetail = "Connected as smartprocure_app to smartprocure_db"
            } else {
                $dbDetail = "Connection failed: $testOut"
            }
        } finally {
            $env:PGPASSWORD = $oldPgp
        }
    } else {
        $dbDetail = "smartprocure_db credentials not yet configured or using placeholder."
    }
}
Add-CheckResult "Database" "Application Role Connectivity" $dbConnected $dbDetail

# 9. Git Secret Exclusion (.gitignore)
$gitIgnored = $false
if (Get-Command git -ErrorAction SilentlyContinue) {
    $ignCheck = git -C "C:\SmartProcure" check-ignore .env 2>&1
    if ($ignCheck -eq ".env") {
        $gitIgnored = $true
    }
}
Add-CheckResult "Security" ".env Excluded from Git" $gitIgnored ".env is properly ignored by .gitignore"

# 10. Git Tracked Secrets Check
$trackedSecrets = git -C "C:\SmartProcure" ls-files | Where-Object { $_ -match '\.env|\.key|\.pem|password' -and $_ -notmatch '\.env\.example' }
$noTrackedSecrets = ($trackedSecrets.Count -eq 0)
Add-CheckResult "Security" "No Secrets in Git Index" $noTrackedSecrets "Tracked sensitive files: $($trackedSecrets.Count)"

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Verification Summary:" -ForegroundColor Cyan
$passes = ($report | Where-Object { $_.Status -eq "PASS" }).Count
$fails = ($report | Where-Object { $_.Status -eq "FAIL" }).Count
Write-Host " Total Passed: $passes" -ForegroundColor Green
if ($fails -gt 0) {
    Write-Host " Total Failed: $fails" -ForegroundColor Red
} else {
    Write-Host " All checks passed!" -ForegroundColor Green
}
Write-Host "==========================================================" -ForegroundColor Cyan
