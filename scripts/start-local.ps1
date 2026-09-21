param(
    [string]$ApiKeyFile,
    [switch]$SyncOnStart,
    [switch]$Background
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if ($ApiKeyFile) {
    $keyContents = (Get-Content -LiteralPath $ApiKeyFile -Raw).Trim()
    $keyValue = ($keyContents -replace '^[^=\r\n]+=', '').Trim().Trim('"').Trim("'")
    if ($keyValue -notmatch '^[A-Za-z0-9]+$') { throw 'API key file must contain one key or SEOUL_API_KEY=value.' }
    $env:SEOUL_API_KEY = $keyValue
}
$env:SYNC_ON_START = if ($SyncOnStart) { 'true' } else { 'false' }
$listenPort = if ($env:PORT) { $env:PORT } else { '8080' }
if (-not $env:APP_BASE_URL) { $env:APP_BASE_URL = 'http://localhost:' + $listenPort }
if (-not $env:JAVA_HOME) {
    $localJdk = Join-Path $env:USERPROFILE '.jdks/ms-21.0.11'
    if (Test-Path -LiteralPath $localJdk) { $env:JAVA_HOME = $localJdk }
}
$javaExe = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$appVersion = (Get-Content -LiteralPath (Join-Path $projectRoot 'VERSION') -Raw).Trim()
$jarFile = Join-Path $projectRoot "build/libs/moamoa-$appVersion.jar"
if (-not (Test-Path -LiteralPath $jarFile)) { throw 'Run .\gradlew.bat bootJar first.' }
if ($Background) {
    $runDir = Join-Path $projectRoot '.runtime'
    New-Item -ItemType Directory -Force -Path $runDir | Out-Null
    $process = Start-Process -FilePath $javaExe -ArgumentList @('-jar', ('"' + $jarFile + '"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runDir 'app.log') -RedirectStandardError (Join-Path $runDir 'app-error.log') -PassThru
    $process.Id | Set-Content -LiteralPath (Join-Path $runDir 'app.pid')
    Write-Output "moamoa starting on http://localhost:$listenPort (PID $($process.Id))."
} else {
    & $javaExe -jar $jarFile
}
