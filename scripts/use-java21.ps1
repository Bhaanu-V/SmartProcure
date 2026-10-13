<#
.SYNOPSIS
    Activates Eclipse Temurin JDK 21 LTS for the current PowerShell session.

.DESCRIPTION
    Discovers the installed JDK 21 directory, points JAVA_HOME to it,
    prepends the JDK 21 bin folder to PATH (overriding any Oracle Java 25 javapath),
    and verifies that both 'java' and 'javac' report version 21.

.EXAMPLE
    . .\scripts\use-java21.ps1
    Dot-sourcing this script applies the environment variables to the active PowerShell terminal.
#>

[CmdletBinding()]
param(
    [string]$ExplicitJdkPath = ""
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [SmartProcure] Activating JDK 21 for Current Session" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

function Find-Jdk21 {
    # 1. If explicit path passed as argument
    if ($ExplicitJdkPath -and (Test-Path "$ExplicitJdkPath\bin\java.exe") -and (Test-Path "$ExplicitJdkPath\bin\javac.exe")) {
        return (Resolve-Path $ExplicitJdkPath).Path
    }

    # 2. Check Machine or User JAVA_HOME if it points to 21
    $candidates = @()
    $machJavaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
    if ($machJavaHome) { $candidates += $machJavaHome.TrimEnd('\') }

    $userJavaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
    if ($userJavaHome) { $candidates += $userJavaHome.TrimEnd('\') }

    # 3. Known standard directories
    $knownRoots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Amazon Corretto",
        "C:\Program Files\Zulu"
    )

    foreach ($root in $knownRoots) {
        if (Test-Path $root) {
            $matchingDirs = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match '21' }
            foreach ($m in $matchingDirs) {
                $candidates += $m.FullName
            }
        }
    }

    # 4. Check registry
    $regPaths = @(
        "HKLM:\SOFTWARE\Eclipse Adoptium\JDK",
        "HKLM:\SOFTWARE\JavaSoft\JDK"
    )
    foreach ($reg in $regPaths) {
        if (Test-Path $reg) {
            $subKeys = Get-ChildItem -Path $reg -ErrorAction SilentlyContinue | Where-Object { $_.PSChildName -match '21' }
            foreach ($k in $subKeys) {
                $pathVal = (Get-ItemProperty -Path $k.PSPath -ErrorAction SilentlyContinue).Path
                if ($pathVal) { $candidates += $pathVal.TrimEnd('\') }
            }
        }
    }

    # Verify candidates
    foreach ($c in ($candidates | Select-Object -Unique)) {
        if ($c -and (Test-Path "$c\bin\java.exe") -and (Test-Path "$c\bin\javac.exe")) {
            $rawVer = cmd.exe /c "`"$c\bin\java.exe`" -version 2>&1"
            if ($rawVer -match 'version "(21[^\"]*)"') {
                return $c
            }
        }
    }

    return $null
}

$jdk21 = Find-Jdk21

if (-not $jdk21) {
    Write-Host "`n[ERROR] Eclipse Temurin JDK 21 LTS could not be found on this system." -ForegroundColor Red
    Write-Host "Searched locations:" -ForegroundColor Yellow
    Write-Host " - C:\Program Files\Eclipse Adoptium"
    Write-Host " - C:\Program Files\Java"
    Write-Host " - Machine and User JAVA_HOME variables"
    Write-Host "`nPlease verify that Eclipse Temurin JDK 21 is installed, or pass the path explicitly:" -ForegroundColor Yellow
    Write-Host "  . .\scripts\use-java21.ps1 -ExplicitJdkPath 'C:\path\to\jdk-21'" -ForegroundColor Yellow
    return
}

# Apply to current session
$env:JAVA_HOME = $jdk21
$jdkBin = Join-Path $jdk21 "bin"

# Prepend JDK 21 bin so it takes precedence over Oracle javapath
$pathList = [System.Collections.Generic.List[string]]::new()
$pathList.Add($jdkBin)
foreach ($p in ($env:PATH -split ';')) {
    $trimmed = $p.Trim()
    if ($trimmed -and ($trimmed -ne $jdkBin)) {
        $pathList.Add($trimmed)
    }
}
$env:PATH = $pathList -join ';'

# Also ensure PostgreSQL 18 client tools are in PATH for convenience if present
$pgBin = "C:\Program Files\PostgreSQL\18\bin"
if ((Test-Path "$pgBin\psql.exe") -and ($env:PATH -notmatch [regex]::Escape($pgBin))) {
    $env:PATH = "$env:PATH;$pgBin"
    Write-Host "[OK] Added PostgreSQL 18 tools to session PATH: $pgBin" -ForegroundColor Green
}

Write-Host "[OK] Discovered JDK 21: $jdk21" -ForegroundColor Green
Write-Host "[OK] Set `$env:JAVA_HOME for session." -ForegroundColor Green
Write-Host "[OK] Prepended JDK 21 bin to session `$env:PATH." -ForegroundColor Green

# Verify java and javac
Write-Host "`n--- Verification ---" -ForegroundColor Cyan
Write-Host "Active 'java.exe' location:"
$activeJava = (Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
if (-not $activeJava) {
    $activeJava = (where.exe java 2>$null | Select-Object -First 1)
}
Write-Host " $activeJava"

$prevEAP = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    Write-Host "Java Runtime Version:"
    & java -version

    Write-Host "Java Compiler Version:"
    & javac -version

    Write-Host "Maven Java Version:"
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        & mvn -version | Select-Object -First 3
    }
} finally {
    $ErrorActionPreference = $prevEAP
}

Write-Host "`n[SUCCESS] Environment is now configured for Java 21 LTS in this session." -ForegroundColor Green
Write-Host "Note: To keep this active in your terminal, always run: . .\scripts\use-java21.ps1" -ForegroundColor Gray
