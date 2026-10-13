<#
.SYNOPSIS
    Loads .env file variables into the current PowerShell session.

.DESCRIPTION
    Parses '.env' in the project root, exports environment variables into
    the current session (like $env:DB_URL, $env:DB_USERNAME, $env:DB_PASSWORD, $env:JAVA_HOME),
    and ensures JAVA_HOME/bin is prepended to PATH.

.EXAMPLE
    . .\scripts\load-env.ps1
#>

[CmdletBinding()]
param(
    [string]$EnvFile = "C:\SmartProcure\.env"
)

if (-not (Test-Path $EnvFile)) {
    Write-Warning "No .env file found at '$EnvFile'. Please copy .env.example or run scripts\setup-db.ps1."
    return
}

$lines = Get-Content -Path $EnvFile
$loadedCount = 0

foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not ($trimmed.StartsWith("#"))) {
        $idx = $trimmed.IndexOf('=')
        if ($idx -gt 0) {
            $key = $trimmed.Substring(0, $idx).Trim()
            $val = $trimmed.Substring($idx + 1).Trim()
            # Remove surrounding quotes if present
            if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
            $loadedCount++
        }
    }
}

Write-Host "[OK] Loaded $loadedCount environment variables from '$EnvFile' into current session." -ForegroundColor Green

# If JAVA_HOME is set, ensure it takes precedence in PATH
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin")) {
    $bin = Join-Path $env:JAVA_HOME "bin"
    $pathList = [System.Collections.Generic.List[string]]::new()
    $pathList.Add($bin)
    foreach ($p in ($env:PATH -split ';')) {
        $t = $p.Trim()
        if ($t -and ($t -ne $bin)) { $pathList.Add($t) }
    }
    $env:PATH = $pathList -join ';'
    Write-Host "[OK] Prepended JAVA_HOME/bin ($bin) to session PATH." -ForegroundColor Green
}
